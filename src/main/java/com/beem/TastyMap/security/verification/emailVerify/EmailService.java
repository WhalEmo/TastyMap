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

        String verificationLinkW = "http://localhost:8081/#verify?token=" + token;
        //String verificationLinkA=baseURL+"/auth/verify?token="+token;
        body =
                "Merhaba,\n\n" +
                        "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n" +
                        verificationLinkW +
                        "\n\nBu link 5 dakika geçerlidir.";
        /*
        if(place == "Android"){
             body =
                    "Merhaba,\n\n" +
                            "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n" +
                            verificationLinkA +
                            "\n\nBu link 5 dakika geçerlidir.";
        }else{
             body =
                    "Merhaba,\n\n" +
                            "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n" +
                            verificationLinkW +
                            "\n\nBu link 5 dakika geçerlidir.";
        }

         */

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
        //emailRepo.delete(emailtoken);
        return "Email doğrulandı!";
    }
}
