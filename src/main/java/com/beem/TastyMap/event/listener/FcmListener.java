package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.FcmNotificationEvent;
import com.beem.TastyMap.notification.FcmService;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.security.device.UserDeviceEntity;
import com.beem.TastyMap.security.device.UserDeviceRepo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FcmListener {
    private final UserDeviceRepo userDeviceRepo;
    private final FcmService fcmService;

    public FcmListener(UserDeviceRepo userDeviceRepo, FcmService fcmService) {
        this.userDeviceRepo = userDeviceRepo;
        this.fcmService = fcmService;
    }

    @EventListener
    public void handleFcm(FcmNotificationEvent event) {
        List<UserDeviceEntity> devices = userDeviceRepo.findByUser_Id(event.getUserId());

        for (UserDeviceEntity device : devices) {
            try {
                if (device.getFcmToken() != null) {
                    fcmService.sendSecurityNotification(
                            device.getFcmToken(),
                            "Güvenlik Uyarısı!",
                            device.getLastCity()+" konumundan hesabınıza yeni bir cihazdan giriş denemesi yapıldı. Siz misiniz?",
                            event.getNotificationId()
                    );
                }
            } catch (Exception e) {
                System.err.println("Cihaz " + device.getId() + " için bildirim gönderilirken hata oluştu:");
                e.printStackTrace();
            }
        }
    }
}