package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.FcmNotificationEvent;
import com.beem.TastyMap.event.model.SecurityAlertEvent;
import com.beem.TastyMap.event.model.SecurityEmailModel;
import com.beem.TastyMap.notification.*;
import com.beem.TastyMap.security.device.UserDeviceEntity;
import com.beem.TastyMap.security.device.UserDeviceRepo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SecurityNotificationHandler {
    private final NotificationRepo notificationRepo;
    private final UserDeviceRepo userDeviceRepo;
    private final ApplicationEventPublisher eventPublisher;

    public SecurityNotificationHandler(NotificationRepo notificationRepo, UserDeviceRepo userDeviceRepo, ApplicationEventPublisher eventPublisher) {
        this.notificationRepo = notificationRepo;
        this.userDeviceRepo = userDeviceRepo;
        this.eventPublisher = eventPublisher;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityAlert(SecurityAlertEvent event) {
        System.out.println("secutirynotıfıctaıohandlera gırdı");
        NotificationEntity n = new NotificationEntity();

        n.setUser(event.getUser());
        n.setDeviceId(event.getDto().getDeviceId());
        n.setUserAgent(event.getUserAgent());
        n.setStatus(Status.PENDING);
        n.setCreatedAt(LocalDateTime.now());
        n.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        n.setLastIpAddress(event.getIp());
        n.setToken(event.getToken());
        n.setUsed(false);

        userDeviceRepo.findByUser_IdAndDeviceId(event.getUser().getId(), event.getDto().getDeviceId())
                .ifPresent(device -> {
                    n.setLastCity(device.getLastCity());
                    n.setTrusted(device.isTrusted());
                });
        NotificationEntity savedNotification =   notificationRepo.saveAndFlush(n);
        eventPublisher.publishEvent(new SecurityEmailModel(event.getUser(), event.getToken()));
        eventPublisher.publishEvent(new FcmNotificationEvent(event.getUser().getId(), savedNotification.getId()));
    }
}
