package com.beem.TastyMap.RegisterLogin;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.Security.CustomUserDetails;
import com.beem.TastyMap.Security.Verification.ServletFilter.JWTUtill;
import com.beem.TastyMap.Notification.NotificationEntity;
import com.beem.TastyMap.Notification.NotificationRepo;
import com.beem.TastyMap.Notification.Status;
import com.beem.TastyMap.Security.RefreshTokenEntity;
import com.beem.TastyMap.Security.RefreshTokenRepo;
import com.beem.TastyMap.Security.Verification.EmailVerify.EmailEntitiy;
import com.beem.TastyMap.Security.Verification.EmailVerify.EmailRepo;
import com.beem.TastyMap.Security.Verification.EmailVerify.EmailService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final NotificationRepo notificationRepo;
    private final EmailRepo emailRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtill jwtUtill;

    public UserService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, NotificationRepo notificationRepo, EmailRepo emailRepo, EmailService emailService, PasswordEncoder passwordEncoder, JWTUtill jwtUtill) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.notificationRepo = notificationRepo;
        this.emailRepo = emailRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtill = jwtUtill;
    }

    @Transactional
    public UserResponseDTO register(UserRequestDTO user){
         if(userRepo.existsByUsername(user.getUsername())){
             throw new CustomExceptions.UserAlreadyExistsException("Kullanıcı adı zaten alınmış.");
         }
         if(userRepo.existsByEmail(user.getEmail())){
             throw new CustomExceptions.UserAlreadyExistsException("Bu email zaten kayıtlı.");
         }
         UserEntity userEntity=new UserEntity();
         userEntity.setBiography(user.getBiography());
         userEntity.setDate(LocalDateTime.now());
         userEntity.setUsername(user.getUsername().trim());
         userEntity.setEmail(user.getEmail().trim());
         userEntity.setName(user.getName().trim());
         userEntity.setSurname(user.getSurname().trim());
         userEntity.setPassword(passwordEncoder.encode(user.getPassword()));
         userEntity.setProfile(user.getProfile());
         userEntity.setRole(user.getRole());
         userEntity.setEmailVerified(false);
         userEntity.setPrivateProfile(user.isPrivateProfile());
         userRepo.save(userEntity);

        String token= UUID.randomUUID().toString();
        EmailEntitiy verification=new EmailEntitiy();
        verification.setUser(userEntity);
        verification.setToken(token);
        verification.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        emailRepo.save(verification);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            emailService.sendVerificationMail(token, user.getEmail());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
        return new UserResponseDTO(userEntity);
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String userAgent) {
        UserEntity user = userRepo.findByUsername(dto.getUsername().trim())
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new CustomExceptions.InvalidCredentialsException("Kullanıcı adı veya Şifre yanlış!");
        }
        if (!user.isEmailVerified()) {
            throw new CustomExceptions.AuthenticationException("Email adresiniz doğrulanmamış");
        }
        Optional<RefreshTokenEntity> existingDevice = refreshTokenRepo
                .findByUserIdAndDeviceIdAndRevokedFalse(user.getId(), dto.getDeviceId());

        if (existingDevice.isPresent()) {
            return createTokensAndLogin(user, dto, userAgent, existingDevice.get());
        }

        boolean hasRecentUserActivity = user.getLastInteractionAt() != null
                && user.getLastInteractionAt().isAfter(LocalDateTime.now().minusDays(30));

        if (hasRecentUserActivity) {
            createOrUpdatePending(user, dto, userAgent);
            return LoginResponseDTO.pendingSecurity(new UserResponseDTO(user));
        }

        return createTokensAndLogin(user, dto, userAgent, null);
    }
    @Transactional
    public void updateLastInteraction(Long userId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı."));

        LocalDate today = LocalDate.now();
        LocalDateTime last = user.getLastInteractionAt();

        if (last == null || !last.toLocalDate().equals(today)) {
            user.setLastInteractionAt(LocalDateTime.now());
             userRepo.save(user);
        }
    }

    @Transactional
    private LoginResponseDTO createTokensAndLogin(UserEntity user, LoginRequestDTO dto, String userAgent, RefreshTokenEntity existingToken) {
        String accessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtill.generateRefreshToken(user.getId(), dto.getDeviceId());
        RefreshTokenEntity refresh = (existingToken != null) ? existingToken : new RefreshTokenEntity();

        refresh.setUserId(user.getId());
        refresh.setToken(refreshToken);
        refresh.setDeviceId(dto.getDeviceId());
        refresh.setUserAgent(userAgent);
        refresh.setExpiryDate(LocalDateTime.now().plusDays(30));
        refresh.setLastUsedAt(LocalDateTime.now());
        refresh.setRevoked(false);

        if (dto.getFcmToken() != null) {
            refresh.setFcmToken(dto.getFcmToken());
        }
        refreshTokenRepo.save(refresh);
        return new LoginResponseDTO(accessToken, refreshToken, new UserResponseDTO(user));
    }

    @Transactional
    private void createOrUpdatePending(
            UserEntity user,
            LoginRequestDTO dto,
            String userAgent
    ) {

        Optional<NotificationEntity> existingPending =
                notificationRepo.findByUserIdAndDeviceIdAndStatus(user.getId(), dto.getDeviceId(), Status.PENDING);
        if (existingPending.isPresent()) {
            return;
        }
        NotificationEntity n = new NotificationEntity();
        n.setUserId(user.getId());
        n.setDeviceId(dto.getDeviceId());
        n.setUserAgent(userAgent);
        n.setStatus(Status.PENDING);
        n.setCreatedAt(LocalDateTime.now());
        n.setExpiresAt(LocalDateTime.now().plusMinutes(10));//10 dakika gecerli
        notificationRepo.save(n);

        // 🔔 SADECE BURADA bildirim at
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Kullanıcı bulunamadı: " + username)
                );
        return new CustomUserDetails(user);
    }
}
