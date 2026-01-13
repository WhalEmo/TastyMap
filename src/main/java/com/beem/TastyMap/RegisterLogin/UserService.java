package com.beem.TastyMap.RegisterLogin;

import com.beem.TastyMap.Exceptions.Exception;
import com.beem.TastyMap.Security.CustomUserDetails;
import com.beem.TastyMap.Security.JWTUtill;
import com.beem.TastyMap.Security.RefreshTokenEntity;
import com.beem.TastyMap.Security.RefreshTokenRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService implements UserDetailsService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtill jwtUtill;

    public UserService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, PasswordEncoder passwordEncoder, JWTUtill jwtUtill) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
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
         userEntity.setEmailVerified(false);
         userRepo.saveAndFlush(userEntity);

        return new UserResponseDTO(userEntity);
    }

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
    UserDetails userDetails = new CustomUserDetails(user);
    String token = jwtUtill.generateAccessToken(userDetails);
    String refreshToken = jwtUtill.generateRefreshToken(user.getUsername());

    RefreshTokenEntity refresh=new RefreshTokenEntity();
    refresh.setUserId(user.getId());
    refresh.setToken(refreshToken);
    refresh.setDeviceId(dto.getDeviceId());
    refresh.setExpiryDate(LocalDateTime.now().plusDays(30));
    refresh.setRevoked(false);
    refreshTokenRepo.saveAndFlush(refresh);

    UserResponseDTO responseDTO  = getUserDtoByUsername(dto.getUsername());
    return new LoginResponseDTO(token,refreshToken,responseDTO);
}

    public UserResponseDTO getUserDtoByUsername(String username){
        UserEntity user=userRepo
                .findByUsername(username)
                .orElseThrow(()->new Exception.AuthenticationException("Kullanıcı adı hatalı."));
        return new UserResponseDTO(user);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepo.findByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException("Kullanıcı bulunamadı"));

        return new CustomUserDetails(user);
    }
    public Long getUserIdByUsername(String username) {
        return userRepo.findByUsername(username)
                .map(UserEntity::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
