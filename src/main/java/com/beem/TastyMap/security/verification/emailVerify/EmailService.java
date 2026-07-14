package com.beem.TastyMap.security.verification.emailVerify;
import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.security.util.IpUtils;
import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import com.beem.TastyMap.security.verification.common.SecurityVerificationChecker;
import com.beem.TastyMap.websocket.EmailVerifyEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailService {
    private final EmailRepo emailRepo;
    private final UserRepo userRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityVerificationChecker securityVerificationChecker;
    private final EmailVerifyEventService emailVerifyEventService;

    public EmailService(EmailRepo emailRepo, UserRepo userRepo, ApplicationEventPublisher eventPublisher, SecurityVerificationChecker securityVerificationChecker, EmailVerifyEventService emailVerifyEventService) {
        this.emailRepo = emailRepo;
        this.userRepo = userRepo;
        this.eventPublisher = eventPublisher;
        this.securityVerificationChecker = securityVerificationChecker;
        this.emailVerifyEventService = emailVerifyEventService;
    }

    private static final int DEVICE_LIMIT = 5;

    private static final int IP_LIMIT = 10;

    private static final int TOKEN_EXPIRY = 10;

    @Value("${app.base-url}")
    private String baseURL;

    /*
    public void sendVerificationMail(String token,String email){
        String subject="Email Doğrulama";
        String body;

        String verificationLinkW = "http://localhost:8081/#verify?token=" + token; //web
        //String verificationLinkA=baseURL+"/auth/verify?token="+token;     //androıd
        body =
                "Merhaba,\n\n" +
                        "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n" +
                        verificationLinkW +
                        "\n\nBu link 10 dakika geçerlidir.";

        SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
        simpleMailMessage.setFrom("beemdevops@gmail.com");
        simpleMailMessage.setTo(email);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(body);
        javaMailSender.send(simpleMailMessage);
    }

     */

    @Transactional
    public String verifyEmail(String token) throws IOException {
        try {
            System.out.println("verifyEmail() çalıştı");
            EmailEntity emailtoken = emailRepo.findByToken(token)
                    .orElseThrow(() -> new CustomExceptions.InvalidException("Doğrulama bağlantısı geçersiz veya daha önce kullanılmış."));


            if (emailtoken.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new CustomExceptions.TokenExpiredException("Doğrulama linkinin süresi dolmuş. Lütfen yeni bir link isteyin.");
            }

            if (emailtoken.isUsed()) {
                throw new CustomExceptions.AlreadyVerifiedException("E-posta adresi zaten doğrulanmış.");
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
                        System.err.println("DEBUG_LOG: WS uyarısı gönderilirken hata oluştu (Muhtemelen soket kapalı): " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            return "Email doğrulandı!";
        } catch (Exception e) {
            System.err.println("verifyEmail() hata verdi:");
            e.printStackTrace();
            throw e;
        }
    }
    @Transactional
    public Long resendVerification(CommonRequestDTO dto) {
        UserEntity user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Eğer e-posta adresi sistemimizde kayıtlıysa, yeni bir doğrulama bağlantısı gönderilmiştir."));

        if (user.isEmailVerified()) {
            throw new CustomExceptions.NotFoundException("Bu hesap zaten doğrulanmış.");
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
            throw new CustomExceptions.InvalidException("Zaten yakın zamanda bir email doğrulama linki talep ettiniz.");
        }
    }

    private boolean isRateLimitExceeded(Long userId, String ipAddress,String deviceId) {
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
