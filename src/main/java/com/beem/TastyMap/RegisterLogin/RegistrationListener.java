package com.beem.TastyMap.RegisterLogin;

import com.beem.TastyMap.Security.Verification.EmailVerify.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistrationListener {
    private final EmailService emailService;

    public RegistrationListener(EmailService emailService) {
        this.emailService = emailService;
    }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegistrationEvent(OnUserRegistrationEvent event) {
        emailService.sendVerificationMail(event.getToken(), event.getUser().getEmail());
    }
}
