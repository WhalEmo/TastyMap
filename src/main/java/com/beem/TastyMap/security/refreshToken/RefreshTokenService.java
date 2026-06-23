package com.beem.TastyMap.security.refreshToken;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.LoginResponseDTO;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.registerLogin.UserResponseDTO;
import com.beem.TastyMap.security.device.UserDeviceService;
import com.beem.TastyMap.security.servletFilter.JWTUtill;
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


    public RefreshTokenService(UserDeviceService userDeviceService, JWTUtill jwtUtill, RefreshTokenRepo refreshTokenRepo, NotificationRepo notificationRepo) {
        this.userDeviceService = userDeviceService;
        this.jwtUtill = jwtUtill;
        this.refreshTokenRepo = refreshTokenRepo;
        this.notificationRepo = notificationRepo;
    }

    @Transactional
    public RefreshTokenResponseDTO refresh(String refreshToken, String deviceId) {

        RefreshTokenEntity rf = refreshTokenRepo
                .findByTokenWithUser(refreshToken)
                .orElseThrow(() ->
                        new CustomExceptions.AuthorizationException("Refresh token geçersiz")
                );
        UserEntity user = rf.getUser();

        if (!jwtUtill.validateRefreshToken(refreshToken)) {
            throw new CustomExceptions.InvalidException("Refresh token geçersiz");
        }

        if (rf.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptions.InvalidException("Refresh token süresi dolmuş");
        }

        if (!rf.getDeviceId().equals(deviceId)) {
            throw new CustomExceptions.AuthorizationException("Bu token farklı cihaza ait");
        }

        String newAccessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());

        Duration total = Duration.between(rf.getCreatedAt(), rf.getExpiryDate());
        Duration remaining = Duration.between(LocalDateTime.now(), rf.getExpiryDate());

        boolean shouldRotate = remaining.toMillis() < (total.toMillis() / 2);
        if (!shouldRotate) {
            return new RefreshTokenResponseDTO(
                    newAccessToken,
                    refreshToken,
                    "basarili"
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
                "basarili"
        );
    }


    @Transactional
    public LoginResponseDTO refreshApproved(ApprovedRefreshRequestDTO dto) {
        System.out.println("Gelen istek  Refreshtokenservıce- DeviceID: " + dto.getDeviceId());
        Optional<NotificationEntity> notificationOpt = notificationRepo
                .findFirstByDeviceIdAndUsedTrueWithUser(dto.getDeviceId());

        if (notificationOpt.isEmpty()) {
            throw new CustomExceptions.NotFoundException("Cihaz için onay isteği bulunamadı");
        }
        NotificationEntity notification = notificationOpt.get();
        if (notification.getStatus() != Status.APPROVED) {
            throw new CustomExceptions.AuthorizationException("Cihaz henüz onaylanmadı, önce e-posta onayını yap!");
        }

        boolean alreadyHasToken = refreshTokenRepo.existsByUser_IdAndDeviceIdAndRevokedFalse(notification.getUser().getId(), dto.getDeviceId());

        if (alreadyHasToken) {
            throw new CustomExceptions.InvalidException("Bu cihaz zaten yetkilendirilmiş");
        }
        userDeviceService.registerOrUpdateDevice(notification.getUser(), dto.getDeviceId(), dto.getUserAgent(), dto.getFcmToken(), true);

        String refreshToken = jwtUtill.generateRefreshToken(notification.getUser().getId(),dto.getDeviceId());

        RefreshTokenEntity rf = new RefreshTokenEntity(
                notification.getUser(),
                refreshToken,
                dto.getDeviceId(),
                LocalDateTime.now().plusDays(30),
                false
        );

        refreshTokenRepo.save(rf);
        String accessToken = jwtUtill.generateAccessToken(notification.getUser().getId(), notification.getUser().getRole());
        notificationRepo.delete(notification);
        return new LoginResponseDTO(accessToken, refreshToken, new UserResponseDTO(notification.getUser()));
        //return new RefreshTokenResponseDTO(accessToken, refreshToken, "basarili");
    }
}
