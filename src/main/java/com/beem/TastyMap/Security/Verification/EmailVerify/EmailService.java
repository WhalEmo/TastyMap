package com.beem.TastyMap.Security.Verification.EmailVerify;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        String verificationLink=baseURL+"/auth/verify?token="+token;
        String body =
                "Merhaba,\n\n" +
                        "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n" +
                        verificationLink +
                        "\n\nBu link 5 dakika geçerlidir.";
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
                .orElseThrow(() -> new SecurityException("Token geçersiz"));
        if(emailtoken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw  new SecurityException("Token süresi dolmuş");
        }
        UserEntity user= emailtoken.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);
        emailRepo.delete(emailtoken);
        return "Email doğrulandı!";
    }
}
