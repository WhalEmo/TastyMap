package com.beem.TastyMap.event.listener;
import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.security.verification.emailVerify.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistrationListener {
    private final JavaMailSender javaMailSender;

    public RegistrationListener(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }
    @Value("${app.base-url}")
    private String baseURL;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegistrationEvent(OnUserRegistrationEvent event) {
        String subject="Email Doğrulama";
        String body;

        String verificationLinkW = "http://localhost:8081/#verify?token=" + event.getToken(); //web
        //String verificationLinkA=baseURL+"/auth/verify?token="+event.getToken();     //androıd
        body =
                "Merhaba,\n\n" +
                        "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n" +
                        verificationLinkW +
                        "\n\nBu link 10 dakika geçerlidir.";

        SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
        simpleMailMessage.setFrom("beemdevops@gmail.com");
        simpleMailMessage.setTo(event.getEmail());
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(body);
        javaMailSender.send(simpleMailMessage);
    }
}
