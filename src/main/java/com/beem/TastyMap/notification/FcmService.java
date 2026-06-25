package com.beem.TastyMap.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public void sendSecurityNotification(String fcmToken, String title, String body, Long notificationId) {

        Message message = Message.builder()
                .setToken(fcmToken)
                .putData("title", title)
                .putData("body", body)
                .putData("type", "SECURITY_ALERT")
                .putData("notificationId", notificationId.toString())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            System.err.println("Cihaz " + fcmToken + " için bildirim gönderilirken hata oluştu:");
            e.printStackTrace();
        }
    }
}