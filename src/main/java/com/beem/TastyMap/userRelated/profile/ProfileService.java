package com.beem.TastyMap.userRelated.profile;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.registerLogin.dto.UserResponseDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenEntity;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRepo;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRequestDTO;
import com.beem.TastyMap.security.device.UserDeviceRepo;
import com.beem.TastyMap.security.token.TokenBlacklistService;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final BlockRepo blockRepo;
    private final TokenBlacklistService tokenBlacklistService;

    public ProfileService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, PasswordEncoder passwordEncoder, BlockRepo blockRepo, TokenBlacklistService tokenBlacklistService) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.blockRepo = blockRepo;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public void updateProfile(UpdateProfileDTO request, Long userId){
        UserEntity user=userRepo.findById(userId)
                .orElseThrow(()->new CustomExceptions.NotFoundException("Kullanıcı bulunamadı/Yetkisiz erişim"));

        if (userRepo.existsByUsernameAndIdNot(request.getUsername(), userId)) {
            throw new CustomExceptions.UserAlreadyExistsException("Bu kullanıcı adı zaten alınmış");
        }
        user.setProfile(request.getProfilephoto());
        user.setName(request.getName().trim());
        user.setUsername(request.getUsername().trim());
        user.setPrivateProfile(request.isPrivate());
        user.setSurname(request.getSurname().trim());
        user.setBiography(request.getBiyografi());
        userRepo.save(user);
    }

    @Transactional
    public void logout(String deviceId, Long userId) {
        refreshTokenRepo.findByUserIdAndDeviceIdAndRevokedFalse(userId, deviceId)
                .ifPresent(rf -> {
                    rf.setRevoked(true);
                    refreshTokenRepo.save(rf);
                });
        tokenBlacklistService.invalidateDeviceSession(userId,deviceId);
    }

    public List<ActiveDeviceDTO> getActiveDevices(Long userId){
        return refreshTokenRepo.findActiveDevices(userId);
    }

    public long getActiveDeviceCount(Long userId) {
        return refreshTokenRepo.countByUser_IdAndRevokedFalse(userId);
    }

    @Transactional
    public void changePassword(ChangePasswordDTO dto, Long userId){
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
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        refreshTokenRepo.revokeAllUserTokensExceptCurrentDevice(userId,dto.getDeviceId());

        List<String> otherDeviceIds = refreshTokenRepo.findActiveDeviceIdsByUserIdExceptCurrent(userId, dto.getDeviceId());

        for (String deviceId : otherDeviceIds) {
            tokenBlacklistService.invalidateDevicePasswordChanged(userId, deviceId);
        }
        userRepo.save(user);
    }
    @Transactional
    public ProfileDTOresponse getProfile(Long userId, Long myId){
        UserEntity user=userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        boolean blocked = blockRepo.existsByBlocker_IdAndBlocked_Id(userId, myId) ||
                blockRepo.existsByBlocker_IdAndBlocked_Id(myId, userId);

        if(blocked){
            return new ProfileDTOresponse(
                    user.getUsername(),
                    user.getName(),
                    null,
                    user.getRole(),
                    user.getBiography(),
                    0,
                   0,
                    0
            );
        }
        return new ProfileDTOresponse(
                user.getUsername(),
                user.getName(),
                user.getProfile(),
                user.getRole(),
                user.getBiography(),
                user.getPostCount(),
                user.getSubscriberCount(),
                user.getSubscribedCount()
        );
    }
    public UserResponseDTO getMe(Long myId){
        UserEntity user=userRepo.findById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));
        return new UserResponseDTO(user);
    }
}
