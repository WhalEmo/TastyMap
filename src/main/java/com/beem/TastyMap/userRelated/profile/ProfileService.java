package com.beem.TastyMap.userRelated.profile;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.registerLogin.dto.UserResponseDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRepo;
import com.beem.TastyMap.security.token.TokenBlacklistService;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messageSource;

    public ProfileService(UserRepo userRepo,
                          RefreshTokenRepo refreshTokenRepo,
                          PasswordEncoder passwordEncoder,
                          BlockRepo blockRepo,
                          TokenBlacklistService tokenBlacklistService,
                          MessageSource messageSource) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.blockRepo = blockRepo;
        this.tokenBlacklistService = tokenBlacklistService;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void updateProfile(UpdateProfileDTO request, Long userId){
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found")));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String trimmedUsername = request.getUsername().trim();
            if (!trimmedUsername.equals(user.getUsername()) &&
                    userRepo.existsByUsernameAndIdNot(trimmedUsername, userId)) {
                throw new CustomExceptions.UserAlreadyExistsException(getMessage("username.already.taken"));
            }
            user.setUsername(trimmedUsername);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getSurname() != null && !request.getSurname().isBlank()) {
            user.setSurname(request.getSurname().trim());
        }

        if (request.getBiography() != null) {
            user.setBiography(request.getBiography().trim());
        }

        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isBlank()) {
            user.setProfile(request.getProfilePhoto());
        }

        userRepo.save(user);
    }

    @Transactional
    public void logout(String deviceId, Long userId) {
        refreshTokenRepo.findByUserIdAndDeviceIdAndRevokedFalse(userId, deviceId)
                .ifPresent(rf -> {
                    rf.setRevoked(true);
                    refreshTokenRepo.save(rf);
                });
        tokenBlacklistService.invalidateDeviceSession(userId, deviceId);
    }

    public List<ActiveDeviceDTO> getActiveDevices(Long userId){
        return refreshTokenRepo.findActiveDevices(userId);
    }

    public long getActiveDeviceCount(Long userId) {
        return refreshTokenRepo.countByUser_IdAndRevokedFalse(userId);
    }

    @Transactional
    public void changePassword(ChangePasswordDTO dto, Long userId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found")));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new CustomExceptions.InvalidCredentialsException(getMessage("password.incorrect"));
        }
        if (!dto.getNewPassword().equals(dto.getAgainNew())) {
            throw new CustomExceptions.InvalidCredentialsException(getMessage("password.mismatch"));
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new CustomExceptions.InvalidCredentialsException(getMessage("password.same.as.old"));
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        // Veritabanındaki diğer cihaz Refresh Token'larını revoke et
        refreshTokenRepo.revokeAllUserTokensExceptCurrentDevice(userId, dto.getDeviceId());

        // REDİS: Şifreyi değiştiren cihaz HARİÇ tüm cihazların erişimini anında kes
        tokenBlacklistService.invalidateUserSessionsExceptCurrentDevice(userId, dto.getDeviceId());

        userRepo.save(user);
    }

    @Transactional
    public ProfileDTOresponse getProfile(Long userId, Long myId){
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found.simple")));

        // ben onu engelledim mi
        boolean blockedByMe = blockRepo.existsByBlocker_IdAndBlocked_Id(myId, userId);

        // o beni engelledi mi?
        boolean blockedMe = blockRepo.existsByBlocker_IdAndBlocked_Id(userId, myId);

        if (blockedByMe || blockedMe) {
            return new ProfileDTOresponse(
                    user.getUsername(),
                    user.getName(),
                    user.getSurname(),
                    null,
                    user.getRole(),
                    user.getBiography(),
                    0,
                    0,
                    0,
                    blockedByMe,
                    blockedMe
            );
        }
        return new ProfileDTOresponse(
                user.getUsername(),
                user.getName(),
                user.getSurname(),
                user.getProfile(),
                user.getRole(),
                user.getBiography(),
                user.getPostCount(),
                user.getSubscriberCount(),
                user.getSubscribedCount(),
                false,
                false
        );
    }

    public UserResponseDTO getMe(Long myId){
        UserEntity user = userRepo.findById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found.simple")));
        return new UserResponseDTO(user);
    }
}