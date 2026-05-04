package com.beem.TastyMap.RegisterLogin;

import com.beem.TastyMap.Security.ApprovedRefreshRequestDTO;
import com.beem.TastyMap.Security.RefreshTokenRequestDTO;
import com.beem.TastyMap.Security.RefreshTokenResponseDTO;
import com.beem.TastyMap.Security.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public UserController(UserService userService, RefreshTokenService refreshTokenService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }
    @PostMapping("/register")
    public UserResponseDTO register( @Valid @RequestBody UserRequestDTO dto){
        return userService.register(dto);
    }
    @PostMapping("/login")
    public LoginResponseDTO login(
            @Valid @RequestBody LoginRequestDTO dto,
            @RequestHeader("User-Agent") String userAgent
    ) {
        return userService.login(dto, userAgent);
    }
    @PostMapping("/active")
    public ResponseEntity<Void> updateLastInteraction(Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        userService.updateLastInteraction(myId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public RefreshTokenResponseDTO refresh(
            @RequestBody RefreshTokenRequestDTO dto
    ) {
        return refreshTokenService.refresh(dto);
    }
    @PostMapping("/refresh/approved")
    public RefreshTokenResponseDTO refreshApproved(
            @RequestBody ApprovedRefreshRequestDTO dto
    ) {
        return refreshTokenService.refreshApproved(dto);
    }
    @PostMapping("/resendMail")
    public ResponseEntity<String> resendMail(
            @RequestParam String email
    ) {
         userService.resendVerification(email);
        return ResponseEntity.ok("Yeni doğrulama linki e-posta adresinize gönderildi.");
    }




}
