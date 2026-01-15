package com.beem.TastyMap.RegisterLogin;

import com.beem.TastyMap.Exceptions.Exception;
import com.beem.TastyMap.Security.CustomUserDetails;
import com.beem.TastyMap.Security.JWTUtill;
import com.beem.TastyMap.Security.Notification.NotificationEntity;
import com.beem.TastyMap.Security.Notification.NotificationRepo;
import com.beem.TastyMap.Security.Notification.Status;
import com.beem.TastyMap.Security.RefreshTokenEntity;
import com.beem.TastyMap.Security.RefreshTokenRepo;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final NotificationRepo notificationRepo;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtill jwtUtill;

    public UserService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, NotificationRepo notificationRepo, PasswordEncoder passwordEncoder, JWTUtill jwtUtill) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.notificationRepo = notificationRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtill = jwtUtill;
    }
    @Transactional
    public UserResponseDTO register(UserRequestDTO user){
         if(userRepo.existsByUsername(user.getUsername())){
             throw new Exception.UserAlreadyExistsException("Kullanıcı adı zaten alınmış.");
         }
         if(userRepo.existsByEmail(user.getEmail())){
             throw new Exception.UserAlreadyExistsException("Bu email zaten kayıtlı.");
         }
         UserEntity userEntity=new UserEntity();
         userEntity.setBiography(user.getBiography());
         userEntity.setDate(LocalDateTime.now());
         userEntity.setUsername(user.getUsername());
         userEntity.setEmail(user.getEmail());
         userEntity.setName(user.getName());
         userEntity.setSurname(user.getSurname());
         userEntity.setPassword(passwordEncoder.encode(user.getPassword()));
         userEntity.setProfile(user.getProfile());
         userEntity.setRole(user.getRole());
         userEntity.setEmailVerified(true);
         userRepo.saveAndFlush(userEntity);

        return new UserResponseDTO(userEntity);
    }
/*
@Transactional
public LoginResponseDTO login(LoginRequestDTO dto){
    UserEntity user = userRepo.findByUsername(dto.getUsername())
            .orElseThrow(()-> new UsernameNotFoundException("Kullanıcı bulunamadı"));

    if (!passwordEncoder.matches(
            dto.getPassword(),
            user.getPassword()
    )) {
        throw new RuntimeException("Şifre hatalı");
    }
    boolean isdevice = refreshTokenRepo.existsByUserIdAndDeviceId(user.getId(), dto.getDeviceId());
    boolean isLogin = refreshTokenRepo.existsByUserId(user.getId());
    if (!isdevice&& isLogin) {
        //yneicihaz
        NotificationEntity gb = new NotificationEntity();
        gb.setUserId(user.getId());
        gb.setDeviceId(dto.getDeviceId());
        gb.setUserAgent(dto.getUserAgent());
        gb.setStatus(Status.PENDING);
        gb.setCreatedAt(LocalDateTime.now());

        notificationRepo.save(gb);
        // 🔔 PUSH / EMAIL AT
        //notificationService.sendNewDeviceAlert(user, gb);
        return LoginResponseDTO.pendingSecurity(new UserResponseDTO(user));
    }

    String role = user.getRole();
    String accessToken = jwtUtill.generateAccessToken(
            user.getId(),
            role
    );
    String refreshToken = jwtUtill.generateRefreshToken(
            user.getId(),
            dto.getDeviceId()
    );
    RefreshTokenEntity refresh=new RefreshTokenEntity();
    refresh.setUserId(user.getId());
    refresh.setToken(refreshToken);
    refresh.setDeviceId(dto.getDeviceId());
    refresh.setUserAgent(dto.getUserAgent());
    refresh.setExpiryDate(LocalDateTime.now().plusDays(30));
    refresh.setRevoked(false);
    refreshTokenRepo.saveAndFlush(refresh);

    UserResponseDTO responseDTO  = getUserDtoByUsername(dto.getUsername());
    return new LoginResponseDTO(accessToken,refreshToken,responseDTO);
}*/
@Transactional
public LoginResponseDTO login(LoginRequestDTO dto, String userAgent) {

    UserEntity user = userRepo.findByUsername(dto.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));

    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
        throw new RuntimeException("Şifre hatalı");
    }


    Optional<RefreshTokenEntity> existingDevice =
            refreshTokenRepo.findByUserIdAndDeviceIdAndRevokedFalse(user.getId(), dto.getDeviceId());

    if (existingDevice.isEmpty()) {
        boolean hasAnyDevice =
                refreshTokenRepo.existsByUserId(user.getId());
        if (hasAnyDevice) {
            createOrUpdatePending(user, dto, userAgent);
            return LoginResponseDTO.pendingSecurity(new UserResponseDTO(user));
        }
        return createTokensAndLogin(user, dto, userAgent);
    }

    RefreshTokenEntity rt = existingDevice.get();

    if (!userAgent.equals(rt.getUserAgent())) {
        createOrUpdatePending(user, dto, userAgent);
        return LoginResponseDTO.pendingSecurity(new UserResponseDTO(user));
    }
    return createTokensAndLogin(user, dto, userAgent);
}

    private LoginResponseDTO createTokensAndLogin(UserEntity user, LoginRequestDTO dto, String userAgent) {
        String accessToken = jwtUtill.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtill.generateRefreshToken(user.getId(), dto.getDeviceId());

        RefreshTokenEntity refresh = new RefreshTokenEntity();
        refresh.setUserId(user.getId());
        refresh.setToken(refreshToken);
        refresh.setDeviceId(dto.getDeviceId());
        refresh.setUserAgent(userAgent);
        refresh.setExpiryDate(LocalDateTime.now().plusDays(30));
        refresh.setRevoked(false);

        refreshTokenRepo.save(refresh);
        return new LoginResponseDTO(accessToken, refreshToken, new UserResponseDTO(user));
    }

    private void createOrUpdatePending(
            UserEntity user,
            LoginRequestDTO dto,
            String userAgent
    ) {
        NotificationEntity n = notificationRepo
                .findByUserIdAndDeviceId(user.getId(), dto.getDeviceId())
                .orElse(new NotificationEntity());

        n.setUserId(user.getId());
        n.setDeviceId(dto.getDeviceId());
        n.setUserAgent(userAgent);
        n.setStatus(Status.PENDING);
        n.setCreatedAt(LocalDateTime.now());

        notificationRepo.save(n);

        // 🔔 burada push / email atılır
    }



    public UserResponseDTO getUserDtoByUsername(String username){
        UserEntity user=userRepo
                .findByUsername(username)
                .orElseThrow(()->new Exception.AuthenticationException("Kullanıcı adı hatalı."));
        return new UserResponseDTO(user);
    }


    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Kullanıcı bulunamadı: " + userId)
                );

        return new CustomUserDetails(user);
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
