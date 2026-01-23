package com.beem.TastyMap.UserProfile;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.Security.RefreshTokenEntity;
import com.beem.TastyMap.Security.RefreshTokenRepo;
import com.beem.TastyMap.Security.RefreshTokenRequestDTO;
import com.beem.TastyMap.UserProfile.Block.BlockRepo;
import com.beem.TastyMap.UserProfile.Subscribe.ProfileDTOresponse;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfileService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final SubscribeRepo subscribeRepo;
    private final BlockRepo blockRepo;

    public ProfileService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, PasswordEncoder passwordEncoder, SubscribeRepo subscribeRepo, BlockRepo blockRepo) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.subscribeRepo = subscribeRepo;
        this.blockRepo = blockRepo;
    }

    public void updateProfile(UpdateProfileDTO request, Long userId){
        UserEntity user=userRepo.findById(userId)
                .orElseThrow(()->new CustomExceptions.NotFoundException("Kullanıcı bulunamadı/Yetkisiz erişim"));

        if (userRepo.existsByUsernameAndIdNot(request.getUsername(), userId)) {
            throw new CustomExceptions.UserAlreadyExistsException("Bu kullanıcı adı zaten alınmış");
        }
        user.setProfile(request.getProfilephoto());
        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setSurname(request.getSurname());
        user.setBiography(request.getBiyografi());
        userRepo.save(user);
    }

    public void logout(RefreshTokenRequestDTO dto, Long userId) {
        RefreshTokenEntity rf = refreshTokenRepo
                .findByTokenAndRevokedFalse(dto.getRefreshToken())
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Refresh token bulunamadı")
                );
        if (!rf.getUserId().equals(userId)) {
            throw new CustomExceptions.AuthorizationException("Yetkisiz erişim.");
        }
        if (!rf.getDeviceId().equals(dto.getDeviceId())) {
            throw new CustomExceptions.InvalidException("Cihaz uyuşmuyor");
        }
        rf.setRevoked(true);
        refreshTokenRepo.save(rf);
    }

    public List<ActiveDeviceDTO> getActiveDevices(Long userId){
        List<RefreshTokenEntity>tokens=refreshTokenRepo
                .findAllByUserIdAndRevokedFalse(userId);

        return tokens.stream().map(rt->new ActiveDeviceDTO(
                rt.getDeviceId(),
                rt.getUserAgent(),
                rt.getLastUsedAt()
        )).toList();
    }

    public long getActiveDeviceCount(Long userId) {
        return refreshTokenRepo.countByUserIdAndRevokedFalse(userId);
    }

    public void changePassword(ChangePasswordDTO dto,Long userId){
        UserEntity user=userRepo.findById(userId)
                .orElseThrow(()->new CustomExceptions.NotFoundException("Kullanıcı bulunamadı/Yetkisiz erişim"));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new CustomExceptions.InvalidCredentialsException("Şifre yanlış!");
        }
        if (!dto.getNewPassword().equals(dto.getAgainNew())) {
            throw new CustomExceptions.InvalidCredentialsException("Şifreler uyuşmuyor!");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new CustomExceptions.InvalidCredentialsException("Yeni şifre eski şifreyle aynı olamaz!");
        }
        user.setLastInteractionAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepo.save(user);
    }

    public ProfileDTOresponse getProfile(Long userId,Long myId){
        UserEntity user=userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        boolean blocked = blockRepo.existsByBlockerIdAndBlockedId(userId, myId) ||
                blockRepo.existsByBlockerIdAndBlockedId(myId, userId);

        if(blocked){
            return new ProfileDTOresponse(
                    user.getUsername(),
                    user.getName(),
                    null,
                    user.getRole(),
                    user.getBiography(),
                    2,
                   0,
                    0
            );
        }
        /daha post yapılmadı

        long subscribedCount = subscribeRepo.countBySubscribedId(userId);
        long subscriberCount = subscribeRepo.countBySubscriberId(userId);
        long postCount = postRepo.countByUserId(userId);

        return new ProfileDTOresponse(
                user.getUsername(),
                user.getName(),
                user.getProfile(),
                user.getRole(),
                user.getBiography(),
                2,
                subscriberCount,
                subscribedCount
        );
    }
}
