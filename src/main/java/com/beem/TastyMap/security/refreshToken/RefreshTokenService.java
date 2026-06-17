package com.beem.TastyMap.security.refreshToken;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.security.device.UserDeviceService;
import com.beem.TastyMap.security.servletFilter.JWTUtill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {
    private final UserRepo userRepo;
    private final UserDeviceService userDeviceService;
    private final JWTUtill jwtUtill;
    private final RefreshTokenRepo refreshTokenRepo;
    private final NotificationRepo notificationRepo;


    public RefreshTokenService(UserRepo userRepo, UserDeviceService userDeviceService, JWTUtill jwtUtill, RefreshTokenRepo refreshTokenRepo, NotificationRepo notificationRepo) {
        this.userRepo = userRepo;
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
    public RefreshTokenResponseDTO refreshApproved(ApprovedRefreshRequestDTO dto) {
        Optional<NotificationEntity> notificationOpt = notificationRepo.findByUser_IdAndDeviceId(dto.getUserId(), dto.getDeviceId());
        if (notificationOpt.isEmpty()) {
            throw new CustomExceptions.NotFoundException("Cihaz için onay isteği bulunamadı");
        }
        NotificationEntity notification = notificationOpt.get();


        if (notification.getStatus() == Status.REJECTED) {
            throw new CustomExceptions.AuthorizationException("Cihaz için onay verilmedi");
        }

        if (notification.getStatus() == Status.PENDING) {
            throw new CustomExceptions.InvalidException("Cihaz için onay bekleniyor");
        }
        UserEntity user = userRepo.findById(dto.getUserId())
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Kullanıcı bulunamadı")
                );

        boolean alreadyHasToken = refreshTokenRepo.existsByUser_IdAndDeviceIdAndRevokedFalse(user.getId(), dto.getDeviceId());

        if (alreadyHasToken) {
            throw new CustomExceptions.InvalidException("Bu cihaz zaten yetkilendirilmiş");
        }
        userDeviceService.registerOrUpdateDevice(user, dto.getDeviceId(), dto.getUserAgent(), dto.getFcmToken(), true);

        String refreshToken = jwtUtill.generateRefreshToken(user.getId(),dto.getDeviceId());

        RefreshTokenEntity rf = new RefreshTokenEntity(
                user,
                refreshToken,
                dto.getDeviceId(),
                LocalDateTime.now().plusDays(30),
                false
        );

        refreshTokenRepo.save(rf);
        String accessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());
        notificationRepo.delete(notification);

        return new RefreshTokenResponseDTO(accessToken, refreshToken, "basarili");
    }
}
