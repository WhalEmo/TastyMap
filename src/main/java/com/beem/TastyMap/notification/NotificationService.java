package com.beem.TastyMap.notification;

import com.beem.TastyMap.exceptions.CustomExceptions;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final NotificationRepo notificationRepo;
    private final MessageSource messageSource;

    public NotificationService(NotificationRepo notificationRepo, MessageSource messageSource) {
        this.notificationRepo = notificationRepo;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void approve(Long notificationId, Long userId) {
        Long ownerId = notificationRepo.findUserIdById(notificationId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("notification.not.found")));

        if (!ownerId.equals(userId)) {
            throw new CustomExceptions.AuthorizationException(getMessage("notification.unauthorized"));
        }
        NotificationEntity notification = notificationRepo.findById(notificationId).get();

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            return;
        }
        if (notification.getStatus() != Status.PENDING) {
            throw new CustomExceptions.InvalidException(getMessage("notification.request.invalid"));
        }

        notification.setStatus(Status.APPROVED);
        notificationRepo.save(notification);
    }

    @Transactional
    public void reject(Long notificationId, Long userId) {
        Long ownerId = notificationRepo.findUserIdById(notificationId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("notification.not.found")));

        if (!ownerId.equals(userId)) {
            throw new CustomExceptions.AuthorizationException(getMessage("notification.unauthorized"));
        }
        NotificationEntity notification = notificationRepo.findById(notificationId).get();

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            return;
        }

        if (notification.getStatus() != Status.PENDING) {
            throw new CustomExceptions.InvalidException(getMessage("notification.request.invalid"));
        }

        notification.setStatus(Status.REJECTED);
        notificationRepo.save(notification);
    }
}