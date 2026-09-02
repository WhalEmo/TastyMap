package com.beem.TastyMap.security.refreshToken;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.dto.LoginResponseDTO;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.registerLogin.dto.UserResponseDTO;
import com.beem.TastyMap.security.device.UserDeviceService;
import com.beem.TastyMap.security.servletFilter.JWTUtill;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {
    private final UserDeviceService userDeviceService;
    private final JWTUtill jwtUtill;
    private final RefreshTokenRepo refreshTokenRepo;
    private final NotificationRepo notificationRepo;
    private final MessageSource messageSource;

    public RefreshTokenService(UserDeviceService userDeviceService,
                               JWTUtill jwtUtill,
                               RefreshTokenRepo refreshTokenRepo,
                               NotificationRepo notificationRepo,
                               MessageSource messageSource) {
        this.userDeviceService = userDeviceService;
        this.jwtUtill = jwtUtill;
        this.refreshTokenRepo = refreshTokenRepo;
        this.notificationRepo = notificationRepo;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public RefreshTokenResponseDTO refresh(String refreshToken, String deviceId) {

        RefreshTokenEntity rf = refreshTokenRepo
                .findByTokenWithUser(refreshToken)
                .orElseThrow(() ->
                        new CustomExceptions.AuthorizationException(getMessage("token.refresh.invalid"))
                );
        UserEntity user = rf.getUser();

        if (!jwtUtill.validateRefreshToken(refreshToken)) {
            throw new CustomExceptions.InvalidException(getMessage("token.refresh.invalid"));
        }

        if (rf.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptions.InvalidException(getMessage("token.refresh.expired"));
        }

        if (!rf.getDeviceId().equals(deviceId)) {
            throw new CustomExceptions.AuthorizationException(getMessage("token.refresh.device.mismatch"));
        }

        String newAccessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole(), deviceId);

        Duration total = Duration.between(rf.getCreatedAt(), rf.getExpiryDate());
        Duration remaining = Duration.between(LocalDateTime.now(), rf.getExpiryDate());

        boolean shouldRotate = remaining.toMillis() < (total.toMillis() / 2);
        if (!shouldRotate) {
            return new RefreshTokenResponseDTO(
                    newAccessToken,
                    refreshToken,
                    getMessage("token.refresh.success")
            );
        }
        rf.setRevoked(true);
        refreshTokenRepo.save(rf);
        String newRefreshToken = jwtUtill.generateRefreshToken(user.getId(), rf.getDeviceId());

        RefreshTokenEntity newRf = new RefreshTokenEntity(
                user,
                newRefreshToken,
                rf.getDeviceId(),
                LocalDateTime.now().plusDays(30),
                false
        );

        refreshTokenRepo.save(newRf);

        return new RefreshTokenResponseDTO(
                newAccessToken,
                newRefreshToken,
                getMessage("token.refresh.success")
        );
    }

    @Transactional
    public LoginResponseDTO refreshApproved(ApprovedRefreshRequestDTO dto) {
        Optional<NotificationEntity> notificationOpt = notificationRepo
                .findFirstByDeviceIdAndIsUsedTrueOrderByCreatedAtDesc(dto.getDeviceId());

        if (notificationOpt.isEmpty()) {
            throw new CustomExceptions.NotFoundException(getMessage("token.refresh.approval.not.found"));
        }
        NotificationEntity notification = notificationOpt.get();

        if (notification.getStatus() != Status.APPROVED) {
            throw new CustomExceptions.AuthorizationException(getMessage("token.refresh.not.approved"));
        }

        boolean alreadyHasToken = refreshTokenRepo.existsByUser_IdAndDeviceIdAndRevokedFalse(notification.getUser().getId(), dto.getDeviceId());

        if (alreadyHasToken) {
            throw new CustomExceptions.InvalidException(getMessage("token.refresh.device.already.authorized"));
        }

        userDeviceService.registerOrUpdateDevice(notification.getUser(), dto.getDeviceId(), dto.getUserAgent(), dto.getFcmToken(), true, null);

        String refreshToken = jwtUtill.generateRefreshToken(notification.getUser().getId(), dto.getDeviceId());

        RefreshTokenEntity rf = new RefreshTokenEntity(
                notification.getUser(),
                refreshToken,
                dto.getDeviceId(),
                LocalDateTime.now().plusDays(30),
                false
        );

        refreshTokenRepo.save(rf);
        String accessToken = jwtUtill.generateAccessToken(notification.getUser().getId(), notification.getUser().getRole(), dto.getDeviceId());
        notificationRepo.delete(notification);

        return new LoginResponseDTO(
                accessToken,
                refreshToken,
                new UserResponseDTO(notification.getUser()),
                getMessage("login.success")
        );

    }
}