package com.beem.TastyMap.RegisterLogin;

import com.beem.TastyMap.Security.ApprovedRefreshRequestDTO;
import com.beem.TastyMap.Security.RefreshTokenRequestDTO;
import com.beem.TastyMap.Security.RefreshTokenResponseDTO;
import com.beem.TastyMap.Security.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto,
            @RequestHeader("User-Agent") String userAgent
    ) {
        LoginResponseDTO loginResponse = userService.login(dto, userAgent);
        if (loginResponse.getAccessToken() != null) {

            ResponseCookie accessCookie = userService.createCookie("access_token", loginResponse.getAccessToken(), 900, "/");
            ResponseCookie refreshCookie = userService.createCookie("refresh_token", loginResponse.getRefreshToken(), 2592000, "/api/users/refresh");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(loginResponse);
        }

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/active")
    public ResponseEntity<Void> updateLastInteraction(Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        userService.updateLastInteraction(myId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refresh(
            @RequestBody RefreshTokenRequestDTO dto
    ) {

        RefreshTokenResponseDTO response = refreshTokenService.refresh(dto);
        ResponseCookie accessCookie = userService.createCookie("access_token", response.getAccessToken(), 900, "/");
        ResponseCookie refreshCookie = userService.createCookie("refresh_token", response.getRefreshtoken(), 2592000, "/api/users/refresh");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/refresh/approved")
    public ResponseEntity<RefreshTokenResponseDTO> refreshApproved(
            @RequestBody ApprovedRefreshRequestDTO dto
    ) {
        RefreshTokenResponseDTO response = refreshTokenService.refreshApproved(dto);

        ResponseCookie accessCookie = userService.createCookie("access_token", response.getAccessToken(), 900, "/");
        ResponseCookie refreshCookie = userService.createCookie("refresh_token", response.getRefreshtoken(), 2592000, "/api/users/refresh");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }
    @PostMapping("/resendMail")
    public ResponseEntity<String> resendMail(
            @RequestParam String email
    ) {
         userService.resendVerification(email);
        return ResponseEntity.ok("Yeni doğrulama linki e-posta adresinize gönderildi.");
    }




}
