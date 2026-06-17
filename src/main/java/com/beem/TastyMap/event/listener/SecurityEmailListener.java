package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.event.model.SecurityEmailModel;
import com.beem.TastyMap.security.verification.pendingRiskVerify.PendingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class SecurityEmailListener {
    private final PendingService pendingService;

    public SecurityEmailListener(PendingService pendingService) {
        this.pendingService = pendingService;
    }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityEvent(SecurityEmailModel event) throws Exception {
        pendingService.sendSecurityAlertMail(event.getToken(),event.getUser().getEmail());
    }
}
