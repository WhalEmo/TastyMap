package com.beem.TastyMap.security.verification.common;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.banned.BanDurationFormatter;
import com.beem.TastyMap.security.banned.BannedDeviceEntity;
import com.beem.TastyMap.security.banned.BannedDeviceRepo;
import com.beem.TastyMap.security.banned.ProgressiveBanPolicy;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SecurityVerificationChecker {

    private final BannedDeviceRepo bannedDeviceRepo;
    private final ProgressiveBanPolicy banPolicy;
    private final MessageSource messageSource;
    private final BanDurationFormatter banDurationFormatter;

    public SecurityVerificationChecker(BannedDeviceRepo bannedDeviceRepo,
                                       ProgressiveBanPolicy banPolicy,
                                       MessageSource messageSource,
                                       BanDurationFormatter banDurationFormatter) {
        this.bannedDeviceRepo = bannedDeviceRepo;
        this.banPolicy = banPolicy;
        this.messageSource = messageSource;
        this.banDurationFormatter = banDurationFormatter;
    }

    private String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    private String getMessage(String code) {
        return getMessage(code, null);
    }

    public void checkIfDeviceIsBanned(Long userId, String deviceId) {
        bannedDeviceRepo.findByUser_IdAndDeviceId(userId, deviceId)
                .ifPresent(ban -> {
                    if (ban.getBannedUntil() == null) {
                        throw new CustomExceptions.AuthorizationException(
                                getMessage("security.device.banned.permanent")
                        );
                    }

                    if (ban.getBannedUntil().isAfter(LocalDateTime.now())) {
                        throw new CustomExceptions.AuthorizationException(
                                getMessage("security.device.banned.temporary")
                        );
                    }
                });
    }

    public void applyProgressiveBan(UserEntity user, CommonRequestDTO dto, String ip) {
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

        bannedDevice.setReason(getMessage("security.ban.reason.excessive"));

        bannedDeviceRepo.save(bannedDevice);

        String durationText = banDurationFormatter.formatBanDuration(bannedUntil);

        throw new CustomExceptions.InvalidException(
                getMessage("security.device.banned.progressive", new Object[]{durationText})
        );
    }
}