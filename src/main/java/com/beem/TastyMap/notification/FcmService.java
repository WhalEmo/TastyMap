package com.beem.TastyMap.notification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public void sendSecurityNotification(String fcmToken, String title, String body, Long notificationId) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .putData("type", "SECURITY_ALERT")
                .putData("notificationId", notificationId.toString())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {

        }
    }
}