package com.beem.TastyMap.registerLogin;
import com.beem.TastyMap.event.model.OnUserRegistrationEvent;
import com.beem.TastyMap.event.model.SecurityAlertEvent;
import com.beem.TastyMap.event.model.SecurityEmailEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.SecurityHistorySummary;
import com.beem.TastyMap.registerLogin.dto.LoginRequestDTO;
import com.beem.TastyMap.registerLogin.dto.LoginResponseDTO;
import com.beem.TastyMap.registerLogin.dto.UserRequestDTO;
import com.beem.TastyMap.registerLogin.dto.UserResponseDTO;
import com.beem.TastyMap.security.banned.BannedDeviceRepo;
import com.beem.TastyMap.security.device.UserDeviceDTO;
import com.beem.TastyMap.security.device.UserDeviceService;
import com.beem.TastyMap.security.risk.BruteForceService;
import com.beem.TastyMap.security.risk.RiskAnalysisService;
import com.beem.TastyMap.security.risk.RiskResult;
import com.beem.TastyMap.security.risk.SecurityValidationService;
import com.beem.TastyMap.security.servletFilter.JWTUtill;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.security.refreshToken.RefreshTokenEntity;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRepo;
import com.beem.TastyMap.security.util.IpUtils;
import com.beem.TastyMap.security.verification.emailVerify.EmailEntitiy;
import com.beem.TastyMap.security.verification.emailVerify.EmailRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
@Service
public class UserService implements UserDetailsService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final NotificationRepo notificationRepo;
    private final BruteForceService bruteForceService;
    private final RiskAnalysisService riskAnalysisService;
    private final UserDeviceService userDeviceService;
    private final EmailRepo emailRepo;
    private final BannedDeviceRepo bannedDeviceRepo;
    private final SecurityValidationService securityValidationService;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtill jwtUtill;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, NotificationRepo notificationRepo, BruteForceService bruteForceService, RiskAnalysisService riskAnalysisService, UserDeviceService userDeviceService, EmailRepo emailRepo, BannedDeviceRepo bannedDeviceRepo, SecurityValidationService securityValidationService, PasswordEncoder passwordEncoder, JWTUtill jwtUtill, ApplicationEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.notificationRepo = notificationRepo;
        this.bruteForceService = bruteForceService;
        this.riskAnalysisService = riskAnalysisService;
        this.userDeviceService = userDeviceService;
        this.emailRepo = emailRepo;
        this.bannedDeviceRepo = bannedDeviceRepo;
        this.securityValidationService = securityValidationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtill = jwtUtill;
        this.eventPublisher = eventPublisher;
    }
    @Value("${app.lockout-minutes}")
    private int lockoutMinutes;


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
        String ip = IpUtils.getClientIp();

        EmailEntitiy verification=new EmailEntitiy();
        verification.setUser(userEntity);
        verification.setToken(token);
        verification.setDeviceId(user.getDeviceId());
        verification.setIpAddress(ip);
        verification.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        emailRepo.save(verification);

        eventPublisher.publishEvent(new OnUserRegistrationEvent(user.getEmail(), token));
        return new UserResponseDTO(userEntity);
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String userAgent) {

        String username = dto.getUsername().trim();
        String deviceId = dto.getDeviceId();
        String ip = IpUtils.getClientIp();

        if (bruteForceService.isBlocked(username)) {
            throw new CustomExceptions.AuthenticationException(
                    "Çok fazla hatalı giriş. 30 dakika bekleyiniz."
            );
        }
        UserEntity user = userRepo.findByUsername(dto.getUsername().trim())
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı adı veya Şifre yanlış!"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            bruteForceService.registerFailedAttempt(username);
            throw new CustomExceptions.InvalidCredentialsException(
                    "Kullanıcı adı veya Şifre yanlış!"
            );
        }

        bruteForceService.resetAttempts(username);

        if (!user.isEmailVerified()) {
            throw new CustomExceptions.AuthenticationException(
                    "Email adresiniz doğrulanmamış"
            );
        }
        if (bannedDeviceRepo.existsByUserIdAndDeviceId(user.getId(), deviceId)) {
            throw new CustomExceptions.AuthorizationException(
                    "Bu cihazdan yapılan şüpheli istekler nedeniyle erişiminiz kalıcı olarak engellenmiştir. " +
                            "Lütfen destek ekibiyle iletişime geçin."
            );
        }

        RiskResult riskResult = riskAnalysisService.calculateRiskScore(user, ip, deviceId);
        UserDeviceDTO cachedDevice = riskResult.getDeviceDto();

        if (riskResult.getScore() != 0){//riskScore >= 70) {
            System.out.println("USerservıce ife gırdı");
            return handleHighRiskLogin(user, dto, userAgent, ip, cachedDevice);
        }
        RefreshTokenEntity existingToken = refreshTokenRepo
                .findByUserIdAndDeviceIdAndRevokedFalse(user.getId(), dto.getDeviceId())
                .orElse(null);

        return createTokensAndLogin(user, dto, userAgent,existingToken,cachedDevice);
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
    private LoginResponseDTO createTokensAndLogin(UserEntity user, LoginRequestDTO dto, String userAgent,RefreshTokenEntity existingToken,UserDeviceDTO cachedDevice) {
        String accessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtill.generateRefreshToken(user.getId(), dto.getDeviceId());
        RefreshTokenEntity refresh = (existingToken != null) ? existingToken : new RefreshTokenEntity();

        refresh.setUser(user);
        refresh.setToken(refreshToken);
        refresh.setDeviceId(dto.getDeviceId());
        refresh.setExpiryDate(LocalDateTime.now().plusDays(30));
        refresh.setRevoked(false);


        userDeviceService.registerOrUpdateDevice(user, dto.getDeviceId(), userAgent, dto.getFcmToken(), true,cachedDevice);
        refreshTokenRepo.save(refresh);
        return new LoginResponseDTO(accessToken, refreshToken, new UserResponseDTO(user));
    }



    private LoginResponseDTO handleHighRiskLogin(UserEntity user, LoginRequestDTO dto, String userAgent, String ip,UserDeviceDTO cachedDevice) {
        String deviceId = dto.getDeviceId();
        LocalDateTime now = LocalDateTime.now();

        SecurityHistorySummary summary = notificationRepo.getSecurityHistorySummary(deviceId, ip,now.minusHours(24));

        securityValidationService.checkThrottlingAndBanRules(user, deviceId, ip, summary);

        boolean hasActivePending = notificationRepo.existsActiveNotificationForUser(user.getId(), Status.PENDING);

        if (!hasActivePending) {
            String token = UUID.randomUUID().toString();

            boolean isTrusted = cachedDevice != null && cachedDevice.isTrusted();

            eventPublisher.publishEvent(new SecurityAlertEvent(user.getId(), user.getEmail(), dto.getDeviceId(),userAgent, ip, token,isTrusted));
        }

        return LoginResponseDTO.pendingSecurity();
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Kullanıcı bulunamadı: " + username)
                );
        return new CustomUserDetails(user);
    }

    protected ResponseCookie createCookie(String name, String value, long maxAge, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(maxAge)
                .sameSite("None")
                .build();
    }
}
