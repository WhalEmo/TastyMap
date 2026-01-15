package com.beem.TastyMap.Security.Notification;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepo notificationRepo;

    public NotificationService(NotificationRepo notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    public void approve(Long notificationId){
        NotificationEntity notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification bulunamadı"));

        notification.setStatus(Status.APPROVED);
        notificationRepo.save(notification);
    }

    public void reject(Long notificationId) {
        NotificationEntity notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification bulunamadı"));

        notification.setStatus(Status.REJECTED);
        notificationRepo.save(notification);
    }

}
