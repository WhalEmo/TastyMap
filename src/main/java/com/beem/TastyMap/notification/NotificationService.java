package com.beem.TastyMap.notification;

import com.beem.TastyMap.exceptions.CustomExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final NotificationRepo notificationRepo;

    public NotificationService(NotificationRepo notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @Transactional
    public void approve(Long notificationId,Long userId) {
        Long ownerId = notificationRepo.findUserIdById(notificationId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Bulunamadı"));

        if (!ownerId.equals(userId)) {
            throw new CustomExceptions.AuthorizationException("Yetkisiz erişim.Bu bildirim sana ait değil");
        }
        NotificationEntity notification = notificationRepo.findById(notificationId).get();

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            return;
        }
        if (notification.getStatus() != Status.PENDING) {
            throw new CustomExceptions.InvalidException("Bu onay isteği artık geçerli değil");
        }

        notification.setStatus(Status.APPROVED);
        notificationRepo.save(notification);
    }
    @Transactional
    public void reject(Long notificationId,Long userId) {
        Long ownerId = notificationRepo.findUserIdById(notificationId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Bulunamadı"));

        if (!ownerId.equals(userId)) {
            throw new CustomExceptions.AuthorizationException("Yetkisiz erişim.Bu bildirim sana ait değil");
        }
        NotificationEntity notification = notificationRepo.findById(notificationId).get();

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            return;
        }

        if (notification.getStatus() != Status.PENDING) {
            throw new CustomExceptions.InvalidException("Bu onay isteği artık geçerli değil");
        }

        notification.setStatus(Status.REJECTED);
        notificationRepo.save(notification);
    }
}
