package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.user.account.model.ClientTypes; // Kendi paket yolunuza göre düzenleyin
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

@Component
public class RegistrationListener {
    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;

    @Value("${app.base-url.api}")
    private String apiBaseUrl;

    @Value("${app.base-url.web}")
    private String webBaseUrl;

    public RegistrationListener(JavaMailSender javaMailSender, MessageSource messageSource) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
    }

    private String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegistrationEvent(OnUserRegistrationEvent event) {
        try {
            Locale locale = LocaleContextHolder.getLocale();

            String verificationLink;

            if (ClientTypes.MOBILE.equalsIgnoreCase(event.getClientType())) {
                verificationLink = apiBaseUrl + "/auth/verify?token=" + event.getToken();
            } else {
                verificationLink = webBaseUrl + "/#verify?token=" + event.getToken();
            }

            String subject = getMessage("email.registration.subject", null, locale);
            String body = getMessage("email.registration.body", new Object[]{verificationLink}, locale);

            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom("beemdevops@gmail.com");
            simpleMailMessage.setTo(event.getEmail());
            simpleMailMessage.setSubject(subject);
            simpleMailMessage.setText(body);

            javaMailSender.send(simpleMailMessage);
        } catch (Exception e) {
            System.err.println("Kayıt maili gönderilirken hata oluştu: " + e.getMessage());
        }
    }
}