package com.beem.TastyMap.security.verification.emailVerify;
import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.security.util.IpUtils;
import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import com.beem.TastyMap.security.verification.common.SecurityVerificationChecker;
import com.beem.TastyMap.security.verification.forgotPassword.PasswordEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailService {
    private final EmailRepo emailRepo;
    private final UserRepo userRepo;
    private final JavaMailSender javaMailSender;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityVerificationChecker securityVerificationChecker;

    public EmailService(EmailRepo emailRepo, UserRepo userRepo, JavaMailSender javaMailSender, ApplicationEventPublisher eventPublisher, SecurityVerificationChecker securityVerificationChecker) {
        this.emailRepo = emailRepo;
        this.userRepo = userRepo;
        this.javaMailSender = javaMailSender;
        this.eventPublisher = eventPublisher;
        this.securityVerificationChecker = securityVerificationChecker;
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
    public String verifyEmail(String token){
        EmailEntitiy emailtoken=emailRepo.findByToken(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Token geçersiz"));


        if(emailtoken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new CustomExceptions.TokenExpiredException("Doğrulama linkinin süresi dolmuş. Lütfen yeni bir link isteyin.");
        }

        if (emailtoken.isUsed()) {
            throw new CustomExceptions.AlreadyVerifiedException("E-posta adresi zaten doğrulanmış.");
        }

        UserEntity user= emailtoken.getUser();
        user.setEmailVerified(true);
        emailtoken.setUsed(true);
        userRepo.save(user);
        return "Email doğrulandı!";
    }
    @Transactional
    public void resendVerification(CommonRequestDTO dto) {
        UserEntity user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı."));

        if (user.isEmailVerified()) {
            throw new CustomExceptions.NotFoundException("Bu hesap zaten doğrulanmış.");
        }
        String ip = IpUtils.getClientIp();

        securityVerificationChecker.checkIfDeviceIsBanned(user.getId(), dto.getDeviceId());

        checkActiveTokenExistence(user.getId());

        if (isRateLimitExceeded(user.getId(), ip, dto.getDeviceId())) {
            securityVerificationChecker.applyProgressiveBan(user, dto,ip);
        }

        String newToken = UUID.randomUUID().toString();
        EmailEntitiy verification = new EmailEntitiy();
        verification.setUser(user);
        verification.setToken(newToken);
        verification.setDeviceId(dto.getDeviceId());
        verification.setIpAddress(ip);
        verification.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY));
        emailRepo.save(verification);

        eventPublisher.publishEvent(new OnUserRegistrationEvent(user.getEmail(), newToken));
    }

    private void checkActiveTokenExistence(Long userId) {
        boolean hasActiveToken = emailRepo.existsByUser_IdAndUsedFalseAndExpiryDateAfter(userId, LocalDateTime.now());
        if (hasActiveToken) {
            throw new CustomExceptions.InvalidException("Zaten yakın zamanda bir şifre sıfırlama linki talep ettiniz.");
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


}
