package com.beem.TastyMap.security.verification.emailVerify;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import org.springframework.beans.factory.annotation.Value;
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

    public EmailService(EmailRepo emailRepo, UserRepo userRepo, JavaMailSender javaMailSender) {
        this.emailRepo = emailRepo;
        this.userRepo = userRepo;
        this.javaMailSender = javaMailSender;
    }
    @Value("${app.base-url}")
    private String baseURL;

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
    public void resendVerification(String email) {
        UserEntity user = userRepo.findByEmail(email)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı."));

        if (user.isEmailVerified()) {
            throw new CustomExceptions.NotFoundException("Bu hesap zaten doğrulanmış.");
        }

        emailRepo.deleteByUser(user);
        String newToken = UUID.randomUUID().toString();
        EmailEntitiy verification = new EmailEntitiy();
        verification.setUser(user);
        verification.setToken(newToken);
        verification.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        emailRepo.save(verification);

        sendVerificationMail(newToken, user.getEmail());
    }
}
