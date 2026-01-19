package com.beem.TastyMap.Security;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.Security.DeviceCount.ActiveDeviceDTO;
import com.beem.TastyMap.Notification.NotificationEntity;
import com.beem.TastyMap.Notification.NotificationRepo;
import com.beem.TastyMap.Notification.Status;
import com.beem.TastyMap.Security.Verification.ServletFilter.JWTUtill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        if (!rf.getDeviceId().equals(dto.getDeviceId())) {
            throw new RuntimeException("Refresh token bu cihaza ait değil");
        }

        refreshTokenRepo.delete(rf);

        String newRefreshToken = jwtUtill.generateRefreshToken(rf.getUserId(), rf.getDeviceId());

        RefreshTokenEntity newRf = new RefreshTokenEntity(
                rf.getUserId(),
                newRefreshToken,
                rf.getDeviceId(),
                rf.getUserAgent(),
                LocalDateTime.now().plusDays(30),
                false,
                rf.getFcmToken(),
                LocalDateTime.now()
        );

        refreshTokenRepo.save(newRf);

        UserEntity user = userRepo.findById(rf.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        String newAccessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());

        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken, "basarili");
    }


    @Transactional
    public RefreshTokenResponseDTO refreshApproved(ApprovedRefreshRequestDTO dto) {
        Optional<NotificationEntity> notificationOpt = notificationRepo.findByUserIdAndDeviceId(dto.getUserId(), dto.getDeviceId());
        if (notificationOpt.isEmpty()) {
            throw new RuntimeException("Cihaz için onay isteği bulunamadı");
        }
        NotificationEntity notification = notificationOpt.get();

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            throw new RuntimeException("Onay süresi dolmuş");
        }

        if (notification.getStatus() == Status.REJECTED) {
            throw new RuntimeException("Cihaz için onay verilmedi");
        }

        if (notification.getStatus() == Status.PENDING) {
            throw new RuntimeException("Cihaz için onay bekleniyor");
        }
        UserEntity user = userRepo.findById(dto.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("Kullanıcı bulunamadı")
                );

        boolean alreadyHasToken = refreshTokenRepo.existsByUserIdAndDeviceIdAndRevokedFalse(user.getId(), dto.getDeviceId());

        if (alreadyHasToken) {
            throw new RuntimeException("Bu cihaz zaten yetkilendirilmiş");
        }

        String refreshToken = jwtUtill.generateRefreshToken(user.getId(),dto.getDeviceId());

        RefreshTokenEntity rf = new RefreshTokenEntity(
                user.getId(),
                refreshToken,
                dto.getDeviceId(),
                dto.getUserAgent(),
                LocalDateTime.now().plusDays(30),
                false,
                dto.getFcmToken(),
                LocalDateTime.now()
        );
        refreshTokenRepo.save(rf);
        String accessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());
        notificationRepo.delete(notification);

        return new RefreshTokenResponseDTO(accessToken, refreshToken, "basarili");
    }


    public void logout(RefreshTokenRequestDTO dto,Long userId) {
        RefreshTokenEntity rf = refreshTokenRepo
                .findByTokenAndRevokedFalse(dto.getRefreshToken())
                .orElseThrow(() ->
                        new RuntimeException("Refresh token bulunamadı")
                );
        if(rf.getUserId().equals(userId)){
            throw new RuntimeException("Yetkisiz erişim.");
        }
        if (!rf.getDeviceId().equals(dto.getDeviceId())) {
            throw new RuntimeException("Cihaz uyuşmuyor");
        }
        rf.setRevoked(true);
        refreshTokenRepo.save(rf);
    }

    public List<ActiveDeviceDTO> getActiveDevices(Long userId){
        List<RefreshTokenEntity>tokens=refreshTokenRepo
                .findAllByUserIdAndRevokedFalse(userId);

        if (tokens.isEmpty()) {
            throw new CustomExceptions.NotFoundException("Aktif oturum bulunamadı.");
        }
        return tokens.stream().map(rt->new ActiveDeviceDTO(
                rt.getDeviceId(),
                rt.getUserAgent(),
                rt.getLastUsedAt()
        )).toList();
    }

    public long getActiveDeviceCount(Long userId) {
        return refreshTokenRepo.countByUserIdAndRevokedFalse(userId);
    }
}
