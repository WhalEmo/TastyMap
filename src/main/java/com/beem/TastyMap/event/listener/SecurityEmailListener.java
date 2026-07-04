package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.event.model.SecurityEmailModel;
import com.beem.TastyMap.security.verification.pendingRiskVerify.PendingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
public class SecurityEmailListener {
    private final PendingService pendingService;

    public SecurityEmailListener(PendingService pendingService) {
        this.pendingService = pendingService;
    }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityEvent(SecurityEmailModel event) throws Exception {
        try {
            pendingService.sendSecurityAlertMail(event.getToken(), event.getUser().getEmail());
        } catch (Exception e) {
          System.out.println("Mail gönderilemedi: "+ e);
        }
    }
}
