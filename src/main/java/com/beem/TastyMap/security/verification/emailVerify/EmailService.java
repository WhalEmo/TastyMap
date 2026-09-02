package com.beem.TastyMap.security.verification.emailVerify;

import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.security.util.IpUtils;
import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import com.beem.TastyMap.security.verification.common.SecurityVerificationChecker;
import com.beem.TastyMap.websocket.EmailVerifyEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class EmailService {
    private final EmailRepo emailRepo;
    private final UserRepo userRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityVerificationChecker securityVerificationChecker;
    private final EmailVerifyEventService emailVerifyEventService;
    private final MessageSource messageSource;

    private static final int DEVICE_LIMIT = 5;
    private static final int IP_LIMIT = 10;
    private static final int TOKEN_EXPIRY = 10;

    @Value("${app.base-url}")
    private String baseURL;

    public EmailService(EmailRepo emailRepo,
                        UserRepo userRepo,
                        ApplicationEventPublisher eventPublisher,
                        SecurityVerificationChecker securityVerificationChecker,
                        EmailVerifyEventService emailVerifyEventService,
                        MessageSource messageSource) {
        this.emailRepo = emailRepo;
        this.userRepo = userRepo;
        this.eventPublisher = eventPublisher;
        this.securityVerificationChecker = securityVerificationChecker;
        this.emailVerifyEventService = emailVerifyEventService;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public String verifyEmail(String token) throws IOException {
        try {
            EmailEntity emailtoken = emailRepo.findByToken(token)
                    .orElseThrow(() -> new CustomExceptions.InvalidException(getMessage("email.token.invalid.or.used")));

            if (emailtoken.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new CustomExceptions.TokenExpiredException(getMessage("email.token.expired"));
            }

            if (emailtoken.isUsed()) {
                throw new CustomExceptions.AlreadyVerifiedException(getMessage("email.already.verified"));
            }

            UserEntity user = emailtoken.getUser();
            user.setEmailVerified(true);
            emailtoken.setUsed(true);
            userRepo.save(user);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        emailVerifyEventService.EmailVerified(emailtoken.getDeviceId());
                    } catch (Exception e) {
                        log.error("DEBUG_LOG: WS uyarısı gönderilirken hata oluştu (Muhtemelen soket kapalı): {}", e.getMessage(), e);
                    }
                }
            });
            return getMessage("email.verified.success");
        } catch (Exception e) {
            log.error("verifyEmail() hata verdi:", e);
            throw e;
        }
    }

    @Transactional
    public Long resendVerification(CommonRequestDTO dto) {
        UserEntity user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("email.resend.not.found")));

        if (user.isEmailVerified()) {
            throw new CustomExceptions.NotFoundException(getMessage("email.resend.already.verified"));
        }
        String ip = IpUtils.getClientIp();

        securityVerificationChecker.checkIfDeviceIsBanned(user.getId(), dto.getDeviceId());

        checkActiveTokenExistence(user.getId());

        if (isRateLimitExceeded(user.getId(), ip, dto.getDeviceId())) {
            securityVerificationChecker.applyProgressiveBan(user, dto, ip);
        }

        String newToken = UUID.randomUUID().toString();
        EmailEntity verification = new EmailEntity();
        verification.setUser(user);
        verification.setToken(newToken);
        verification.setDeviceId(dto.getDeviceId());
        verification.setIpAddress(ip);
        verification.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY));
        emailRepo.save(verification);

        eventPublisher.publishEvent(new OnUserRegistrationEvent(user.getEmail(), newToken));

        return user.getId();
    }

    private void checkActiveTokenExistence(Long userId) {
        boolean hasActiveToken = emailRepo.existsByUser_IdAndUsedFalseAndExpiryDateAfter(userId, LocalDateTime.now());
        if (hasActiveToken) {
            throw new CustomExceptions.InvalidException(getMessage("email.resend.already.requested"));
        }
    }

    private boolean isRateLimitExceeded(Long userId, String ipAddress, String deviceId) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusDays(1);

        long deviceRequestCount = emailRepo.countByUserIdAndDeviceIdAndCreatedAtAfter(userId, deviceId, twentyFourHoursAgo);
        if (deviceRequestCount >= DEVICE_LIMIT) {
            return true;
        }

        long ipRequestCount = emailRepo.countByIpAddressAndCreatedAtAfter(ipAddress, twentyFourHoursAgo);
        if (ipRequestCount >= IP_LIMIT) {
            return true;
        }

        return false;
    }

    public boolean isUsedEmail(Long userId) {
        return userRepo.existsByIdAndEmailVerifiedTrue(userId);
    }
}