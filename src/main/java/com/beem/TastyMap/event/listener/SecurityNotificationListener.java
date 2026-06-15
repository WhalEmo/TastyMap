package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.event.model.SecurityAlertEvent;
import com.beem.TastyMap.notification.*;
import com.beem.TastyMap.security.device.UserDeviceEntity;
import com.beem.TastyMap.security.device.UserDeviceRepo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SecurityNotificationListener {
    private final NotificationRepo notificationRepo;
    private final UserDeviceRepo userDeviceRepo;
    private final FcmService fcmService;

    public SecurityNotificationListener(NotificationRepo notificationRepo, UserDeviceRepo userDeviceRepo, FcmService fcmService) {
        this.notificationRepo = notificationRepo;
        this.userDeviceRepo = userDeviceRepo;
        this.fcmService = fcmService;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityAlert(SecurityAlertEvent event) {
        NotificationEntity n = new NotificationEntity();

        n.setUser(event.getUser());
        n.setDeviceId(event.getDto().getDeviceId());
        n.setUserAgent(event.getUserAgent());
        n.setStatus(Status.PENDING);
        n.setCreatedAt(LocalDateTime.now());
        n.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        n.setLastIpAddress(event.getIp());

        userDeviceRepo.findByUser_IdAndDeviceId(event.getUser().getId(), event.getDto().getDeviceId())
                .ifPresent(device -> {
                    n.setLastCity(device.getLastCity());
                    n.setTrusted(device.isTrusted());
                });
        NotificationEntity savedNotification = notificationRepo.save(n);

        List<UserDeviceEntity> devices = userDeviceRepo.findByUser_Id(event.getUser().getId());

        for (UserDeviceEntity device : devices) {
            try {
                if (device.getFcmToken() != null) {
                    fcmService.sendSecurityNotification(
                            device.getFcmToken(),
                            "Güvenlik Uyarısı!",
                            "Hesabınıza yeni bir cihazdan giriş denemesi yapıldı. Siz misiniz?",
                            savedNotification.getId()
                    );
                }
            } catch (Exception e) {
                System.err.println("Cihaz " + device.getId() + " için bildirim gönderilirken hata oluştu:");
                e.printStackTrace();
            }
        }

    }
}
