package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.SecurityEmailEvent;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

@Component
public class SecurityEmailListener {
    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;

    public SecurityEmailListener(JavaMailSender javaMailSender, MessageSource messageSource) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
    }

    @Value("${app.base-url}")
    private String baseURL;

    private String getMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, locale);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityEvent(SecurityEmailEvent event) throws Exception {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("beemdevops@gmail.com");
            helper.setTo(event.getEmail());
            helper.setSubject(getMessage("email.security.subject", locale));

            String approveLink = baseURL + "/auth/verifyLogin?token=" + event.getToken() + "&action=approve";
            String rejectLink = baseURL + "/auth/verifyLogin?token=" + event.getToken() + "&action=reject";

            String htmlBody = """
<div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
    <h2 style="color: #001970;">%s</h2>
    <p>%s</p>
    <p style="color: #d9534f; font-size: 14px; font-weight: bold; margin-bottom: 20px;">%s</p>
    <div style="margin: 30px 0;">
        <a href="%s" style="background-color: #28a745; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">%s</a>
        <a href="%s" style="background-color: #dc3545; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; margin-left: 15px;">%s</a>
    </div>
    <p style="color: #666; font-size: 12px;">%s</p>
</div>
""".formatted(
                    getMessage("email.security.title", locale),
                    getMessage("email.security.body", locale),
                    getMessage("email.security.warning", locale),
                    approveLink,
                    getMessage("email.security.button.approve", locale),
                    rejectLink,
                    getMessage("email.security.button.reject", locale),
                    getMessage("email.security.footer", locale)
            );

            helper.setText(htmlBody, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            System.out.println("Mail gönderilemedi: " + e);
        }
    }
}