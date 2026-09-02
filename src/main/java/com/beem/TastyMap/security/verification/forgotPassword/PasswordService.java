package com.beem.TastyMap.security.verification.forgotPassword;

import com.beem.TastyMap.event.model.PasswordMailEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRepo;
import com.beem.TastyMap.security.util.IpUtils;
import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import com.beem.TastyMap.security.verification.common.SecurityVerificationChecker;
import com.beem.TastyMap.websocket.PasswordEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PasswordService {
    private final UserRepo userRepo;
    private final PasswordRepo passwordRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepo refreshTokenRepo;
    private final SecurityVerificationChecker securityVerificationChecker;
    private final PasswordEventService passwordEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;

    private static final int DEVICE_LIMIT = 5;
    private static final int IP_LIMIT = 10;
    private static final int TOKEN_EXPIRY = 10;

    public PasswordService(UserRepo userRepo,
                           PasswordRepo passwordRepo,
                           PasswordEncoder passwordEncoder,
                           RefreshTokenRepo refreshTokenRepo,
                           SecurityVerificationChecker securityVerificationChecker,
                           PasswordEventService passwordEventService,
                           ApplicationEventPublisher eventPublisher,
                           MessageSource messageSource) {
        this.userRepo = userRepo;
        this.passwordRepo = passwordRepo;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
        this.securityVerificationChecker = securityVerificationChecker;
        this.passwordEventService = passwordEventService;
        this.eventPublisher = eventPublisher;
        this.messageSource = messageSource;
    }

    @Value("${app.base-url}")
    private String baseURL;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public PasswordResetResponse forgotPassword(CommonRequestDTO dto) {
        UserEntity user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("password.reset.not.found")));

        String ip = IpUtils.getClientIp();

        securityVerificationChecker.checkIfDeviceIsBanned(user.getId(), dto.getDeviceId());

        checkActiveTokenExistence(user.getId());

        if (isRateLimitExceeded(user.getId(), ip, dto.getDeviceId())) {
            securityVerificationChecker.applyProgressiveBan(user, dto, ip);
        }

        generateTokenAndSendMail(user, ip, dto.getDeviceId());
        return new PasswordResetResponse(
                user.getId(),
                dto.getDeviceId(),
                getMessage("password.reset.email.sent")
        );
    }

    private void checkActiveTokenExistence(Long userId) {
        boolean hasActiveToken = passwordRepo.existsByUser_IdAndUsedFalseAndExpiryDateAfter(userId, LocalDateTime.now());
        if (hasActiveToken) {
            throw new CustomExceptions.InvalidException(getMessage("password.reset.already.requested"));
        }
    }

    private boolean isRateLimitExceeded(Long userId, String ipAddress, String deviceId) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusDays(1);

        long deviceRequestCount = passwordRepo.countByUserIdAndDeviceIdAndCreatedAtAfter(userId, deviceId, twentyFourHoursAgo);
        if (deviceRequestCount >= DEVICE_LIMIT) {
            return true;
        }

        long ipRequestCount = passwordRepo.countByIpAddressAndCreatedAtAfter(ipAddress, twentyFourHoursAgo);
        if (ipRequestCount >= IP_LIMIT) {
            return true;
        }

        return false;
    }

    private void generateTokenAndSendMail(UserEntity user, String ip, String deviceId) {
        String token = UUID.randomUUID().toString();
        PasswordEntity passwordEntity = new PasswordEntity();
        passwordEntity.setToken(token);
        passwordEntity.setUser(user);
        passwordEntity.setUsed(false);
        passwordEntity.setIpAddress(ip);
        passwordEntity.setDeviceId(deviceId);
        passwordEntity.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY));
        passwordRepo.save(passwordEntity);

        eventPublisher.publishEvent(new PasswordMailEvent(user.getEmail(), token));
    }

    @Transactional
    public String newPassword(ResetPasswordDTO resetDTO) throws IOException {
        PasswordEntity passwordEntity = validateAndGetToken(resetDTO.getToken());
        UserEntity user = passwordEntity.getUser();
        user.setPassword(passwordEncoder.encode(resetDTO.getNewPassword()));

        userRepo.save(user);

        passwordEntity.setUsed(true);
        passwordRepo.saveAndFlush(passwordEntity);

        refreshTokenRepo.revokeAllByUser(user.getId());

        String deviceId = passwordEntity.getDeviceId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    passwordEventService.PasswordChange(deviceId);
                } catch (Exception e) {
                    log.error("DEBUG_LOG: WS uyarısı gönderilirken hata oluştu (Muhtemelen soket kapalı): {}", e.getMessage(), e);
                }
            }
        });

        return getMessage("password.reset.success");
    }

    public PasswordEntity validateAndGetToken(String token) {
        PasswordEntity passwordEntity = passwordRepo.findByToken(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException(getMessage("password.token.invalid")));

        if (passwordEntity.isUsed() || passwordEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptions.InvalidException(getMessage("password.token.invalid.or.expired"));
        }
        return passwordEntity;
    }

    public boolean isUsedPassword(Long userId) {
        return passwordRepo.existsByUser_IdAndUsedTrue(userId);
    }
}