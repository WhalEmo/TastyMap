package com.beem.TastyMap.Security.Verification.ForgotPassword;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.Security.RefreshTokenRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordService {
    private final UserRepo userRepo;
    private final PasswordRepo passwordRepo;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepo refreshTokenRepo;

    public PasswordService(UserRepo userRepo, PasswordRepo passwordRepo, JavaMailSender javaMailSender, PasswordEncoder passwordEncoder, RefreshTokenRepo refreshTokenRepo) {
        this.userRepo = userRepo;
        this.passwordRepo = passwordRepo;
        this.javaMailSender = javaMailSender;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
    }
    @Value("${app.base-url}")
    private String baseURL;

    @Transactional
    public void forgotPassword(String email){
        userRepo.findByEmail(email).ifPresent(user -> {
            passwordRepo.deleteAllByUserId(user.getId());

            String token= UUID.randomUUID().toString();

            PasswordEntity passwordEntity=new PasswordEntity();
            passwordEntity.setToken(token);
            passwordEntity.setUser(user);
            passwordEntity.setExpiryDate(LocalDateTime.now().plusMinutes(5));
            passwordRepo.save(passwordEntity);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendMail(token, email);
                        }
                    }
            );
        });
    }
    public void sendMail(String token,String email){
        String subject="Şifre Sıfırlama Talebi";
        String resetLink=baseURL+"/auth/resetPassword?resetpasswordtoken="+token;
        String body =
                "Merhaba,\n\n" +
                        "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın:\n\n" +
                        resetLink +
                        "\n\nBu bağlantı 5 dakika boyunca geçerlidir.\n" +
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
        PasswordEntity passwordEntity=passwordRepo.findByToken(token)
                .orElseThrow(() -> new SecurityException("Token geçersiz"));
        if (passwordEntity.getExpiryDate().isBefore(LocalDateTime.now())){
            throw  new SecurityException("Token süresi dolmuş");
        }
        UserEntity user=passwordEntity.getUser();
        user.setPassword(passwordEncoder.encode(resetDTO.getNewPassword()));

        userRepo.save(user);
        passwordRepo.delete(passwordEntity);

        refreshTokenRepo.revokeAllByUser(user.getId());
        return ("Şifre değiştirildi.");
    }

}
