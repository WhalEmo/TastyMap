package com.beem.TastyMap.security.risk;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.SecurityHistorySummary;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.banned.BannedDeviceEntity;
import com.beem.TastyMap.security.banned.BannedDeviceRepo;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SecurityValidationService {

    private final BannedDeviceRepo bannedDeviceRepo;
    private final NotificationRepo notificationRepo;
    private final MessageSource messageSource;

    public SecurityValidationService(BannedDeviceRepo bannedDeviceRepo,
                                     NotificationRepo notificationRepo,
                                     MessageSource messageSource) {
        this.bannedDeviceRepo = bannedDeviceRepo;
        this.notificationRepo = notificationRepo;
        this.messageSource = messageSource;
    }

    private String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    private String getMessage(String code) {
        return getMessage(code, null);
    }

    public void checkThrottlingAndBanRules(UserEntity user, String deviceId, String ip, SecurityHistorySummary summary) {
        LocalDateTime now = LocalDateTime.now();

        long ipAttackCount = summary.getIpAttackCount() != null ? summary.getIpAttackCount() : 0L;
        long rejectCount = summary.getRejectCount() != null ? summary.getRejectCount() : 0L;
        long pendingCount = summary.getPendingCount() != null ? summary.getPendingCount() : 0L;

        if (ipAttackCount >= 10) {
            throw new CustomExceptions.AuthorizationException(getMessage("security.risk.ip.excessive"));
        }

        if (rejectCount >= 5) {
            bannedDeviceRepo.save(createBannedDevice(user, deviceId, ip, now, "MFA Fatigue / Excess Reject Notifications"));
            throw new CustomExceptions.AuthorizationException(getMessage("security.risk.device.permanent"));
        }

        int dynamicLockoutMinutes = (rejectCount == 4) ? 120 :
                (rejectCount == 3) ? 60 :
                        (rejectCount == 2) ? 30 : 0;

        if (dynamicLockoutMinutes > 0) {
            notificationRepo.findLastRejectedTime(deviceId).ifPresent(lastRejectedTime -> {
                LocalDateTime blockUntil = lastRejectedTime.plusMinutes(dynamicLockoutMinutes);
                if (now.isBefore(blockUntil)) {
                    throw new CustomExceptions.AuthorizationException(
                            getMessage("security.risk.login.lockout", new Object[]{dynamicLockoutMinutes})
                    );
                }
            });
        }

        if (pendingCount >= 5) {
            bannedDeviceRepo.save(createBannedDevice(user, deviceId, ip, now, "MFA Fatigue / Excess Pending Notifications"));
            throw new CustomExceptions.AuthorizationException(getMessage("security.risk.device.permanent"));
        }

        int mailThrottlingMinutes = (pendingCount == 4) ? 120 :
                (pendingCount == 3) ? 60 :
                        (pendingCount == 2) ? 30 : 0;

        if (mailThrottlingMinutes > 0) {
            boolean isMailThrottled = notificationRepo.existsByDeviceIdAndCreatedAtAfter(deviceId, now.minusMinutes(mailThrottlingMinutes));
            if (isMailThrottled) {
                throw new CustomExceptions.AuthorizationException(
                        getMessage("security.risk.mail.throttled", new Object[]{mailThrottlingMinutes})
                );
            }
        }
    }

    private BannedDeviceEntity createBannedDevice(UserEntity user, String deviceId, String ip, LocalDateTime now, String reason) {
        BannedDeviceEntity bannedDevice = new BannedDeviceEntity();
        bannedDevice.setUser(user);
        bannedDevice.setDeviceId(deviceId);
        bannedDevice.setLastIpAddress(ip);
        bannedDevice.setBannedAt(now);
        bannedDevice.setReason(reason);
        return bannedDevice;
    }
}