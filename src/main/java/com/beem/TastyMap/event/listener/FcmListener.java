package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.FcmNotificationEvent;
import com.beem.TastyMap.notification.FcmService;
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
        List<String> fcmTokens = userDeviceRepo.findActiveFcmTokensByUserId(event.getUserId());

        if (fcmTokens.isEmpty()) {
            System.out.println("⚠️ Kullanıcı ID " + event.getUserId() + " için aktif FCM token bulunamadı, bildirim atlanıyor.");
            return;
        }

        for (String token : fcmTokens) {
            try {
                fcmService.sendSecurityNotification(
                        token,
                        "Güvenlik Uyarısı!",
                        event.getCity() + " konumundan hesabınıza yeni bir cihazdan giriş denemesi yapıldı. Siz misiniz?",
                        event.getNotificationId()
                );
                System.out.println("✅ Bildirim bir cihaza başarıyla gönderildi.");
            } catch (Exception e) {
                System.err.println("Bir cihaza bildirim gönderilemedi, sonraki cihaza geçiliyor: " + e.getMessage());
            }
        }
    }
}