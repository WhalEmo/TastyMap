package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.PasswordMailEvent;
import com.beem.TastyMap.event.model.SecurityEmailEvent;
import com.beem.TastyMap.security.verification.forgotPassword.PasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordMailListener {
    private final JavaMailSender javaMailSender;

    @Value("${app.base-url}")
    private String baseURL;

    public PasswordMailListener(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordEvent(PasswordMailEvent event){
        String subject="Şifre Sıfırlama Talebi";
        String resetLink= baseURL + "/auth/resetPassword/validate?token=" + event.getToken();
        String body =
                "Merhaba,\n\n" +
                        "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın:\n\n" +
                        resetLink +
                        "\n\nBu bağlantı 10 dakika boyunca geçerlidir.\n" +
                        "Eğer bu isteği siz yapmadıysanız, lütfen bu e-postayı dikkate almayın.";

        SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
        simpleMailMessage.setFrom("beemdevops@gmail.com");
        simpleMailMessage.setTo(event.getEmail());
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(body);
        javaMailSender.send(simpleMailMessage);
    }
}
