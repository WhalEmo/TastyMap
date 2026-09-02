package com.beem.TastyMap.security.verification.pendingRiskVerify;

import com.beem.TastyMap.event.model.SecurityEmailEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.*;
import com.beem.TastyMap.security.risk.SecurityValidationService;
import com.beem.TastyMap.websocket.LoginSecureEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PendingService {
    private final NotificationRepo notificationRepo;
    private final LoginSecureEventService loginSecureEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityValidationService securityValidationService;
    private final MessageSource messageSource;

    public PendingService(NotificationRepo notificationRepo,
                          LoginSecureEventService loginSecureEventService,
                          ApplicationEventPublisher eventPublisher,
                          SecurityValidationService securityValidationService,
                          MessageSource messageSource) {
        this.notificationRepo = notificationRepo;
        this.loginSecureEventService = loginSecureEventService;
        this.eventPublisher = eventPublisher;
        this.securityValidationService = securityValidationService;
        this.messageSource = messageSource;
    }

    private static final int TOKEN_EXPIRY = 10;

    @Value("${app.base-url}")
    private String baseURL;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void verifyToken(String token, String action) throws IOException {
        NotificationEntity notification = notificationRepo.findByTokenWithUser(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException(getMessage("security.token.invalid")));

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            throw new CustomExceptions.TokenExpiredException(getMessage("security.token.expired"));
        }

        if (notification.isUsed()) {
            throw new CustomExceptions.AlreadyVerifiedException(getMessage("security.already.verified"));
        }

        if ("approve".equals(action)) {
            notification.setStatus(Status.APPROVED);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        loginSecureEventService.loginApproved(notification.getDeviceId());
                    } catch (Exception e) {
                        log.error("DEBUG_LOG: WS uyarısı gönderilirken hata oluştu (Muhtemelen soket kapalı): {}", e.getMessage(), e);
                    }
                }
            });

        } else {
            notification.setStatus(Status.REJECTED);
            notification.setUpdatedAt(LocalDateTime.now());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        loginSecureEventService.loginRejected(notification.getDeviceId());
                    } catch (Exception e) {
                        log.error("DEBUG_LOG: WS uyarısı gönderilirken hata oluştu (Muhtemelen soket kapalı): {}", e.getMessage(), e);
                    }
                }
            });
        }

        notification.setUsed(true);
        notificationRepo.save(notification);
    }

    @Transactional
    public String resendSecurityAlertMail(String deviceId) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        NotificationEntity notification = notificationRepo.findFirstByDeviceIdAndIsUsedFalseOrderByCreatedAtDesc(deviceId)
                .orElseThrow(() -> new CustomExceptions.InvalidException(getMessage("security.no.pending.approval")));

        if (notification.getStatus() == Status.APPROVED || notification.getStatus() == Status.REJECTED) {
            throw new CustomExceptions.AlreadyVerifiedException(getMessage("security.already.completed"));
        }

        if (notification.getExpiresAt().isAfter(now)) {
            throw new CustomExceptions.AlreadyVerifiedException(getMessage("security.active.email.exists"));
        }
        SecurityHistorySummary summary = notificationRepo.getSecurityHistorySummary(deviceId, notification.getLastIpAddress(), now.minusHours(24));

        securityValidationService.checkThrottlingAndBanRules(notification.getUser(), deviceId, notification.getLastIpAddress(), summary);

        NotificationEntity newNotification =
                notification.createResendNotification(
                        UUID.randomUUID().toString(),
                        now.plusMinutes(TOKEN_EXPIRY)
                );

        notification.setUsed(true);
        notificationRepo.save(notification);

        notificationRepo.save(newNotification);

        eventPublisher.publishEvent(new SecurityEmailEvent(newNotification.getUser().getEmail(), newNotification.getToken()));
        return getMessage("security.email.sent");
    }

    public NotificationResponse isUsedNotification(String deviceId){
        NotificationStatusSummary notification = notificationRepo.findLatestNotificationStatus(deviceId)
                .orElseThrow(() -> new CustomExceptions.InvalidException(getMessage("security.no.active.request")));

        return new NotificationResponse(notification.getStatus(), notification.isUsed());
    }
}