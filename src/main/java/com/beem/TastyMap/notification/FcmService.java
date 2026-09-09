package com.beem.TastyMap.notification;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FcmService {

    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {

        // 1. Tüm verileri tek bir Map içinde topluyoruz (Notification yerine Data Payload)
        Map<String, String> dataPayload = new HashMap<>();

        // Gelen ekstra veri varsa ekle (userId, type vb.)
        if (data != null && !data.isEmpty()) {
            dataPayload.putAll(data);
        }

        // Title ve Body'i de data payload'ına ekliyoruz
        dataPayload.put("title", title);
        dataPayload.put("body", body);

        // 2. iOS cihazların arka planda uyanıp bildirimi işlemesi için APNs konfigürasyonu
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setContentAvailable(true) // iOS arka plan tetikleyicisi (Background Fetch)
                        .build())
                .build();

        // 3. Mesajı OLUŞTUR
        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(dataPayload)
                .setApnsConfig(apnsConfig) // APNs ayarı eklendi
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            System.err.println("Cihaz " + fcmToken + " için bildirim gönderilirken hata oluştu:");
            e.printStackTrace();
        }
    }
}