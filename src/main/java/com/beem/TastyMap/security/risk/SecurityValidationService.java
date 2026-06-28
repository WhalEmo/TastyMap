package com.beem.TastyMap.security.risk;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.banned.BannedDeviceEntity;
import com.beem.TastyMap.security.banned.BannedDeviceRepo;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityValidationService {

    private final BannedDeviceRepo bannedDeviceRepo;
    private final NotificationRepo notificationRepo;

    public SecurityValidationService(BannedDeviceRepo bannedDeviceRepo, NotificationRepo notificationRepo) {
        this.bannedDeviceRepo = bannedDeviceRepo;
        this.notificationRepo = notificationRepo;
    }

    public void checkThrottlingAndBanRules(UserEntity user, String deviceId, String ip, List<NotificationEntity> history24Hours) {
        LocalDateTime now = LocalDateTime.now();
        long rejectCount = history24Hours.stream().filter(n -> n.getStatus() == Status.REJECTED).count();

        if (rejectCount >= 5) {
            bannedDeviceRepo.save(createBannedDevice(user, deviceId, ip, now, "MFA Fatigue / Excess Reject Notifications"));
            throw new CustomExceptions.AuthorizationException(
                    "Bu cihazdan yapılan şüpheli istekler nedeniyle erişiminiz kalıcı olarak engellenmiştir. Lütfen destek ekibiyle iletişime geçin."
            );
        }
        int dynamicLockoutMinutes = (rejectCount == 4) ? 120 :
                (rejectCount == 3) ? 60 :
                        (rejectCount == 2) ? 30 : 0;

        if (dynamicLockoutMinutes > 0) {
            history24Hours.stream()
                    .filter(n -> n.getStatus() == Status.REJECTED)
                    .findFirst()
                    .ifPresent(lastRejected -> {
                        LocalDateTime blockUntil = lastRejected.getUpdatedAt().plusMinutes(dynamicLockoutMinutes);
                        if (now.isBefore(blockUntil)) {
                            throw new CustomExceptions.AuthorizationException(
                                    String.format("Bu cihazdan yapılan girişler üst üste reddedildi. Lütfen %d dakika sonra tekrar deneyiniz.", dynamicLockoutMinutes)
                            );
                        }
                    });
        }

        long pendingCount = history24Hours.stream()
                .filter(n -> n.getStatus() == Status.PENDING || n.getStatus() == Status.EXPIRED).count();

        if (pendingCount >= 5) {
            bannedDeviceRepo.save(createBannedDevice(user, deviceId, ip, now, "MFA Fatigue / Excess Pending Notifications"));
            throw new CustomExceptions.AuthorizationException(
                    "Bu cihazdan yapılan şüpheli istekler nedeniyle erişiminiz kalıcı olarak engellenmiştir. Lütfen destek ekibiyle iletişime geçin."
            );
        }

        int mailThrottlingMinutes = (pendingCount == 4) ? 120 :
                (pendingCount == 3) ? 60 :
                        (pendingCount == 2) ? 30 : 0;

        if (mailThrottlingMinutes > 0) {
            boolean isMailThrottled = notificationRepo.existsByDeviceIdAndCreatedAtAfter(deviceId, now.minusMinutes(mailThrottlingMinutes));
            if (isMailThrottled) {
                throw new CustomExceptions.AuthorizationException(
                        String.format("Çok sık şüpheli giriş isteği üretildi. Lütfen %d dakika sonra tekrar deneyiniz.", mailThrottlingMinutes)
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
