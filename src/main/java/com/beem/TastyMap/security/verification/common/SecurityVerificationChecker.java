package com.beem.TastyMap.security.verification.common;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.banned.BanDurationFormatter;
import com.beem.TastyMap.security.banned.BannedDeviceEntity;
import com.beem.TastyMap.security.banned.BannedDeviceRepo;
import com.beem.TastyMap.security.banned.ProgressiveBanPolicy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SecurityVerificationChecker {

    private final BannedDeviceRepo bannedDeviceRepo;
    private final ProgressiveBanPolicy banPolicy;

    public SecurityVerificationChecker(BannedDeviceRepo bannedDeviceRepo, ProgressiveBanPolicy banPolicy) {
        this.bannedDeviceRepo = bannedDeviceRepo;
        this.banPolicy = banPolicy;
    }

    public void checkIfDeviceIsBanned(Long userId, String deviceId) {
        bannedDeviceRepo.findByUser_IdAndDeviceId(userId, deviceId)
                .ifPresent(ban -> {
                    if (ban.getBannedUntil() == null) {
                        throw new CustomExceptions.AuthorizationException(
                                "Bu cihaz kalıcı olarak engellenmiştir. Daha fazla bilgi almak için lütfen destek ekibimizle iletişime geçin."
                        );
                    }

                    if (ban.getBannedUntil().isAfter(LocalDateTime.now())) {
                        throw new CustomExceptions.AuthorizationException(
                                "Bu cihaz güvenlik nedeniyle geçici olarak engellenmiştir. Lütfen daha sonra tekrar deneyin."
                        );
                    }
                });
    }


    public void applyProgressiveBan(UserEntity user, CommonRequestDTO dto ,String ip) {
        BannedDeviceEntity bannedDevice = bannedDeviceRepo
                .findByUser_IdAndDeviceId(user.getId(), dto.getDeviceId())
                .orElse(new BannedDeviceEntity());

        bannedDevice.setUser(user);
        bannedDevice.setDeviceId(dto.getDeviceId());
        bannedDevice.setLastIpAddress(ip);

        int previousViolations = bannedDevice.getViolationCount();
        bannedDevice.setViolationCount(previousViolations + 1);

        LocalDateTime bannedUntil = banPolicy.calculateBanReleaseTime(previousViolations);
        bannedDevice.setBannedUntil(bannedUntil);

        bannedDevice.setReason("Excessive password reset requests");

        bannedDeviceRepo.save(bannedDevice);
        throw new CustomExceptions.InvalidException(
                "Güvenlik nedeniyle bu cihazın erişimi "
                        + BanDurationFormatter.formatBanDuration(bannedUntil)
                        + " süreyle geçici olarak engellenmiştir."
        );
    }
}
