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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordService {
    private final UserRepo userRepo;
    private final PasswordRepo passwordRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepo refreshTokenRepo;
    private final SecurityVerificationChecker securityVerificationChecker;
    private final PasswordEventService passwordEventService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int DEVICE_LIMIT = 5;

    private static final int IP_LIMIT = 10;

    private static final int TOKEN_EXPIRY = 10;

    public PasswordService(UserRepo userRepo, PasswordRepo passwordRepo, PasswordEncoder passwordEncoder, RefreshTokenRepo refreshTokenRepo, SecurityVerificationChecker securityVerificationChecker, PasswordEventService passwordEventService, ApplicationEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.passwordRepo = passwordRepo;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
        this.securityVerificationChecker = securityVerificationChecker;
        this.passwordEventService = passwordEventService;
        this.eventPublisher = eventPublisher;
    }

    @Value("${app.base-url}")
    private String baseURL;

    @Transactional
    public PasswordResetResponse forgotPassword(CommonRequestDTO dto) {
        UserEntity user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Eğer e-posta sistemimizde kayıtlı ise şifre sıfırlama bağlantısı gönderilmiştir."));
        /*
        if(!user.isEmailVerified()){
            throw new CustomExceptions.AuthenticationException("E postanız henüz doğrulanmamış.");
        }

         */
        String ip = IpUtils.getClientIp();

        securityVerificationChecker.checkIfDeviceIsBanned(user.getId(), dto.getDeviceId());

        checkActiveTokenExistence(user.getId());

        if (isRateLimitExceeded(user.getId(), ip, dto.getDeviceId())) {
            securityVerificationChecker.applyProgressiveBan(user, dto , ip);
        }

        generateTokenAndSendMail(user,ip,dto.getDeviceId());
        return new PasswordResetResponse(
                user.getId(),
                dto.getDeviceId(),
                "Şifre sıfırlama bağlantısı e-posta adresinize iletilmiştir. Bağlantı güvenliğiniz için 5 dakika geçerlidir, lütfen e-posta kutunuzu kontrol ediniz."
        );
    }

    private void checkActiveTokenExistence(Long userId) {
        boolean hasActiveToken = passwordRepo.existsByUser_IdAndUsedFalseAndExpiryDateAfter(userId, LocalDateTime.now());
        if (hasActiveToken) {
            throw new CustomExceptions.InvalidException("Zaten yakın zamanda bir şifre sıfırlama linki talep ettiniz.");
        }
    }

    private boolean isRateLimitExceeded(Long userId, String ipAddress,String deviceId) {
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

    private void generateTokenAndSendMail(UserEntity user,String ip, String deviceId) {
        String token = UUID.randomUUID().toString();
        PasswordEntity passwordEntity = new PasswordEntity();
        passwordEntity.setToken(token);
        passwordEntity.setUser(user);
        passwordEntity.setUsed(false);
        passwordEntity.setIpAddress(ip);
        passwordEntity.setDeviceId(deviceId);
        passwordEntity.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY));
        passwordRepo.save(passwordEntity);

       eventPublisher.publishEvent(new PasswordMailEvent(user.getEmail(),token));
    }
    /*
    @Async
    public void sendMail(String token,String email){
        String subject="Şifre Sıfırlama Talebi";
        String resetLink= baseURL + "/auth/resetPassword/validate?token=" + token;
        String body =
                "Merhaba,\n\n" +
                        "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın:\n\n" +
                        resetLink +
                        "\n\nBu bağlantı 10 dakika boyunca geçerlidir.\n" +
                        "Eğer bu isteği siz yapmadıysanız, lütfen bu e-postayı dikkate almayın.";

        SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
        simpleMailMessage.setFrom("beemdevops@gmail.com");
        simpleMailMessage.setTo(email);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(body);
        javaMailSender.send(simpleMailMessage);
    }

     */

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
                    System.err.println("DEBUG_LOG: WS uyarısı gönderilirken hata oluştu (Muhtemelen soket kapalı): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        return "Şifreniz başarıyla değiştirilmiştir.";
    }


    public PasswordEntity validateAndGetToken(String token) {
        PasswordEntity passwordEntity = passwordRepo.findByToken(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Token geçersiz"));

        if (passwordEntity.isUsed() || passwordEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptions.InvalidException("Token geçersiz veya süresi dolmuş");
        }
        return passwordEntity;
    }

    public boolean isUsedPassword(Long userId) {
        return passwordRepo.existsByUser_IdAndUsedTrue(userId);
    }
}
