package com.beem.TastyMap.Security;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.RegisterLogin.UserService;
import com.beem.TastyMap.Security.Notification.NotificationEntity;
import com.beem.TastyMap.Security.Notification.NotificationRepo;
import com.beem.TastyMap.Security.Notification.Status;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {
    private final UserRepo userRepo;
    private final JWTUtill jwtUtill;
    private final RefreshTokenRepo refreshTokenRepo;
    private final NotificationRepo notificationRepo;

    public RefreshTokenService(UserRepo userRepo, JWTUtill jwtUtill, RefreshTokenRepo refreshTokenRepo, NotificationRepo notificationRepo) {
        this.userRepo = userRepo;
        this.jwtUtill = jwtUtill;
        this.refreshTokenRepo = refreshTokenRepo;
        this.notificationRepo = notificationRepo;
    }
    @Transactional
    public RefreshTokenResponseDTO refresh(RefreshTokenRequestDTO dto) {

        RefreshTokenEntity rf = refreshTokenRepo
                .findByTokenAndRevokedFalse(dto.getRefreshToken())
                .orElseThrow(() ->
                        new RuntimeException("Refresh token bulunamadı veya iptal edilmiş")
                );

        if (!jwtUtill.validateRefreshToken(dto.getRefreshToken())) {
            throw new RuntimeException("Refresh token geçersiz");
        }

        if (rf.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepo.delete(rf);
            throw new RuntimeException("Refresh token süresi dolmuş");
        }

        if (rf.getDeviceId().equals(dto.getDeviceId())) {

            refreshTokenRepo.delete(rf);

            String newRefreshToken =
                    jwtUtill.generateRefreshToken(rf.getUserId(), rf.getDeviceId());

            RefreshTokenEntity newRf = new RefreshTokenEntity(
                    rf.getUserId(),
                    newRefreshToken,
                    rf.getDeviceId(),
                    rf.getUserAgent(),
                    LocalDateTime.now().plusDays(30),
                    false
            );

            refreshTokenRepo.save(newRf);

            UserEntity user = userRepo.findById(rf.getUserId())
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

            String newAccessToken =
                    jwtUtill.generateAccessToken(user.getId(), user.getRole());

            return new RefreshTokenResponseDTO(
                    newAccessToken,
                    newRefreshToken,
                    "basarili"
            );
        }

        NotificationEntity notification = notificationRepo
                .findByUserIdAndDeviceId(rf.getUserId(), dto.getDeviceId())
                .orElseGet(NotificationEntity::new);

        notification.setUserId(rf.getUserId());
        notification.setDeviceId(dto.getDeviceId());
        notification.setStatus(Status.PENDING);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepo.save(notification);

        return new RefreshTokenResponseDTO(null, null, "Cihaz onayı bekleniyor");
    }

    @Transactional
    public void logout(RefreshTokenRequestDTO dto) {
        RefreshTokenEntity rf = refreshTokenRepo
                .findByTokenAndRevokedFalse(dto.getRefreshToken())
                .orElseThrow(() ->
                        new RuntimeException("Refresh token bulunamadı")
                );

        if (!rf.getDeviceId().equals(dto.getDeviceId())) {
            throw new RuntimeException("Cihaz uyuşmuyor");
        }
        rf.setRevoked(true);
        refreshTokenRepo.save(rf);
    }
}
