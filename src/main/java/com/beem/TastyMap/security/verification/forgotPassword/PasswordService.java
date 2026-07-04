package com.beem.TastyMap.security.verification.forgotPassword;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.security.banned.BannedDeviceEntity;
import com.beem.TastyMap.security.banned.BannedDeviceRepo;
import com.beem.TastyMap.security.banned.ProgressiveBanPolicy;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRepo;
import com.beem.TastyMap.security.risk.RiskAnalysisService;
import com.beem.TastyMap.security.risk.RiskResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordService {
    private final UserRepo userRepo;
    private final PasswordRepo passwordRepo;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepo refreshTokenRepo;
    private final BannedDeviceRepo bannedDeviceRepo;
    private final ProgressiveBanPolicy banPolicy;

    public PasswordService(UserRepo userRepo, PasswordRepo passwordRepo, JavaMailSender javaMailSender, PasswordEncoder passwordEncoder, RefreshTokenRepo refreshTokenRepo, BannedDeviceRepo bannedDeviceRepo, ProgressiveBanPolicy banPolicy) {
        this.userRepo = userRepo;
        this.passwordRepo = passwordRepo;
        this.javaMailSender = javaMailSender;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
        this.bannedDeviceRepo = bannedDeviceRepo;
        this.banPolicy = banPolicy;
    }
    @Value("${app.base-url}")
    private String baseURL;

    @Transactional
    public void forgotPassword(PasswordRequestDTO dto) {
        UserEntity user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kayıtlı email adresi bulunamadı."));

        checkIfDeviceIsBanned(user.getId(), dto.getDeviceId());

        checkActiveTokenExistence(user.getId());

        if (isRateLimitExceeded(user.getId())) {
            applyProgressiveBan(user, dto);
        }

        generateTokenAndSendMail(user, dto.getEmail());
    }

    private void checkIfDeviceIsBanned(Long userId, String deviceId) {
        bannedDeviceRepo.findByUser_IdAndDeviceId(userId, deviceId)
                .ifPresent(ban -> {
                    if (ban.getBannedUntil() != null && ban.getBannedUntil().isAfter(LocalDateTime.now())) {
                        throw new CustomExceptions.AuthorizationException("Bu cihaz geçici olarak engellenmiştir.");
                    }
                });
    }

    private void checkActiveTokenExistence(Long userId) {
        boolean hasActiveToken = passwordRepo.existsByUser_IdAndUsedFalseAndExpiryDateAfter(userId, LocalDateTime.now());
        if (hasActiveToken) {
            throw new CustomExceptions.InvalidException("Zaten yakın zamanda bir şifre sıfırlama linki talep ettiniz.");
        }
    }

    private boolean isRateLimitExceeded(Long userId) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusDays(1);
        long requestCount = passwordRepo.countByUserIdAndCreatedAtAfter(userId, twentyFourHoursAgo);
        return requestCount >= 5;
    }

    private void applyProgressiveBan(UserEntity user, PasswordRequestDTO dto) {
        BannedDeviceEntity bannedDevice = bannedDeviceRepo
                .findByUser_IdAndDeviceId(user.getId(), dto.getDeviceId())
                .orElse(new BannedDeviceEntity());

        bannedDevice.setUser(user);
        bannedDevice.setDeviceId(dto.getDeviceId());
        bannedDevice.setLastIpAddress(dto.getCurrentIp());

        int previousViolations = bannedDevice.getViolationCount();
        bannedDevice.setViolationCount(previousViolations + 1);

        // Temiz Mimari: Hesaplamayı Policy sınıfına devrettik
        bannedDevice.setBannedUntil(banPolicy.calculateBanReleaseTime(previousViolations));
        bannedDevice.setReason("Excessive password reset requests");

        bannedDeviceRepo.save(bannedDevice);

        throw new CustomExceptions.InvalidException("Bu cihaz geçici olarak güvenlik nedeniyle engellendi.");
    }

    private void generateTokenAndSendMail(UserEntity user, String email) {
        String token = UUID.randomUUID().toString();
        PasswordEntity passwordEntity = new PasswordEntity();
        passwordEntity.setToken(token);
        passwordEntity.setUser(user);
        passwordEntity.setUsed(false);
        passwordEntity.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        passwordRepo.save(passwordEntity);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendMail(token, email);
            }
        });
    }
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

    @Transactional
    public String newPassword(String token,ResetPasswordDTO resetDTO){
        PasswordEntity passwordEntity = validateAndGetToken(token);

        UserEntity user=passwordEntity.getUser();
        user.setPassword(passwordEncoder.encode(resetDTO.getNewPassword()));

        userRepo.save(user);

        passwordEntity.setUsed(true);
        passwordRepo.save(passwordEntity);

        refreshTokenRepo.revokeAllByUser(user.getId());
        return ("Şifre değiştirildi.");
    }

    public PasswordEntity validateAndGetToken(String token) {
        PasswordEntity passwordEntity = passwordRepo.findByToken(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Token geçersiz"));

        if (passwordEntity.isUsed() || passwordEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptions.InvalidException("Token geçersiz veya süresi dolmuş");
        }
        return passwordEntity;
    }
}
