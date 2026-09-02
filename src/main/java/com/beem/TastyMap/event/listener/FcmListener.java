package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.FcmNotificationEvent;
import com.beem.TastyMap.notification.FcmService;
import com.beem.TastyMap.security.device.UserDeviceRepo;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class FcmListener {
    private final UserDeviceRepo userDeviceRepo;
    private final FcmService fcmService;
    private final MessageSource messageSource;

    public FcmListener(UserDeviceRepo userDeviceRepo, FcmService fcmService, MessageSource messageSource) {
        this.userDeviceRepo = userDeviceRepo;
        this.fcmService = fcmService;
        this.messageSource = messageSource;
    }

    private String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }

    @EventListener
    public void handleFcm(FcmNotificationEvent event) {
        List<String> fcmTokens = userDeviceRepo.findActiveFcmTokensByUserId(event.getUserId());

        if (fcmTokens.isEmpty()) {
            System.out.println("⚠️ Kullanıcı ID " + event.getUserId() + " için aktif FCM token bulunamadı, bildirim atlanıyor.");
            return;
        }

        Locale locale = LocaleContextHolder.getLocale();
        String title = getMessage("notification.fcm.security.title", null, locale);
        String body = getMessage("notification.fcm.security.body", new Object[]{event.getCity()}, locale);

        for (String token : fcmTokens) {
            try {
                fcmService.sendSecurityNotification(
                        token,
                        title,
                        body,
                        event.getNotificationId()
                );
                System.out.println("✅ Bildirim bir cihaza başarıyla gönderildi.");
            } catch (Exception e) {
                System.err.println("Bir cihaza bildirim gönderilemedi, sonraki cihaza geçiliyor: " + e.getMessage());
            }
        }
    }
}