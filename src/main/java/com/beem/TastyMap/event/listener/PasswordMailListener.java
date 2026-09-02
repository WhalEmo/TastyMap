package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.PasswordMailEvent;
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
public class PasswordMailListener {
    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;

    @Value("${app.base-url}")
    private String baseURL;

    public PasswordMailListener(JavaMailSender javaMailSender, MessageSource messageSource) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
    }

    private String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordEvent(PasswordMailEvent event) {
        Locale locale = LocaleContextHolder.getLocale();

        String resetLinkB = "http://localhost:8081/#reset?token=" + event.getToken();

        String subject = getMessage("email.password.reset.subject", null, locale);
        String body = getMessage("email.password.reset.body", new Object[]{resetLinkB}, locale);

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("beemdevops@gmail.com");
        simpleMailMessage.setTo(event.getEmail());
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(body);
        javaMailSender.send(simpleMailMessage);
    }
}