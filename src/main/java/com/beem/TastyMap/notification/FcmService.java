package com.beem.TastyMap.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FcmService {

    public void sendNotification(String fcmToken, String title, String body,  Map<String, String> data) {

        Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                        com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                );

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }
        try {
            FirebaseMessaging.getInstance().send(messageBuilder.build());
        } catch (FirebaseMessagingException e) {
            System.err.println("Cihaz " + fcmToken + " için bildirim gönderilirken hata oluştu:");
            e.printStackTrace();
        }
    }
}