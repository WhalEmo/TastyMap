package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.FcmNotificationEvent;
import com.beem.TastyMap.event.model.SecurityAlertEvent;
import com.beem.TastyMap.event.model.SecurityEmailEvent;
import com.beem.TastyMap.notification.*;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.security.Location.GeoLocationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map; // MAP EKLENDİ

@Component
public class SecurityNotificationHandler {
    private final NotificationRepo notificationRepo;
    private final GeoLocationService geoLocationService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepo userRepo;

    public SecurityNotificationHandler(NotificationRepo notificationRepo, GeoLocationService geoLocationService, ApplicationEventPublisher eventPublisher, UserRepo userRepo) {
        this.notificationRepo = notificationRepo;
        this.geoLocationService = geoLocationService;
        this.eventPublisher = eventPublisher;
        this.userRepo = userRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityAlert(SecurityAlertEvent event) {
        System.out.println("secutirynotıfıctaıohandlera gırdı");
        NotificationEntity n = new NotificationEntity();
        UserEntity user = userRepo.getReferenceById(event.getUserId());
        String city = geoLocationService.getCity(event.getIp());

        n.setUser(user);
        n.setDeviceId(event.getDeviceId());
        n.setUserAgent(event.getUserAgent());
        n.setStatus(Status.PENDING);
        n.setCreatedAt(LocalDateTime.now());
        n.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        n.setLastIpAddress(event.getIp());
        n.setToken(event.getToken());
        n.setUsed(false);
        n.setLastCity(city);
        n.setTrusted(event.isTrusted());

        NotificationEntity savedNotification = notificationRepo.saveAndFlush(n);
        eventPublisher.publishEvent(new SecurityEmailEvent(event.getEmail(),event.getToken()));


        Map<String, String> payloadData = Map.of(
                "type", "SECURITY_ALERT",
                "notificationId", savedNotification.getId().toString()
        );

        // 2. Yeni jenerik FcmNotificationEvent'imizi fırlatıyoruz
        eventPublisher.publishEvent(new FcmNotificationEvent(
                event.getUserId(),
                "notification.fcm.security.title", // messages.properties dosyasındaki başlık kodun
                "notification.fcm.security.body",  // messages.properties dosyasındaki içerik kodun
                new Object[]{city},                // "Şehir" bilgisini çeviri dosyasında {0} olan yere basmak için
                payloadData
        ));
    }
}