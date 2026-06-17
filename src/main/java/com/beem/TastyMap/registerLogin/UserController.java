package com.beem.TastyMap.registerLogin;

import com.beem.TastyMap.security.refreshToken.ApprovedRefreshRequestDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRequestDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenResponseDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenService;
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
            @RequestHeader("User-Agent") String userAgent,
            @RequestHeader(value = "X-Client-Type", defaultValue = ClientTypes.WEB) String clientType
    ) {
        LoginResponseDTO loginResponse = userService.login(dto, userAgent);

        if (ClientTypes.MOBILE.equalsIgnoreCase(clientType)) {
            return ResponseEntity.ok().body(loginResponse);
        } else {
            ResponseCookie accessCookie = userService.createCookie("access_token", loginResponse.getAccessToken(), 900, "/");
            ResponseCookie refreshCookie = userService.createCookie("refresh_token", loginResponse.getRefreshToken(), 2592000, "/api/users/refresh");

            return ResponseEntity.ok()
                    .headers(headers -> {
                        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
                        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                    })
                    .body(new LoginResponseDTO("Giriş başarılı"));
        }

    }

    @PostMapping("/active")
    public ResponseEntity<Void> updateLastInteraction(Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        userService.updateLastInteraction(myId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refresh(
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequestDTO dto,
            @RequestHeader(value = "X-Client-Type", defaultValue = "WEB") String clientType
    ) {

        String finalToken = (ClientTypes.MOBILE.equalsIgnoreCase(clientType) && dto != null) ? dto.getRefreshToken() : cookieRefreshToken;
        String finalDeviceId = (dto != null) ? dto.getDeviceId() : "WEB_CLIENT";

        if (finalToken == null || finalToken.isEmpty()) {
            return ResponseEntity.status(401).body(new RefreshTokenResponseDTO("Refresh token bulunamadı"));
        }

        RefreshTokenResponseDTO response = refreshTokenService.refresh(finalToken, finalDeviceId);

        if (ClientTypes.MOBILE.equalsIgnoreCase(clientType)) {
            return ResponseEntity.ok().body(response);
        } else {
            ResponseCookie accessCookie = userService.createCookie("access_token", response.getAccessToken(), 900, "/");
            ResponseCookie refreshCookie = userService.createCookie("refresh_token", response.getRefreshtoken(), 2592000, "/api/users/refresh");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(new RefreshTokenResponseDTO("Token başarıyla yenilendi"));
        }

    }

    @PostMapping("/refresh/approved")
    public ResponseEntity<RefreshTokenResponseDTO> refreshApproved(
            @RequestBody ApprovedRefreshRequestDTO dto,
            @RequestHeader(value = "X-Client-Type", defaultValue = "WEB") String clientType
    ) {
        RefreshTokenResponseDTO response = refreshTokenService.refreshApproved(dto);

        if (ClientTypes.MOBILE.equalsIgnoreCase(clientType)) {
            return ResponseEntity.ok().body(response);
        } else {
            ResponseCookie accessCookie = userService.createCookie("access_token", response.getAccessToken(), 900, "/");
            ResponseCookie refreshCookie = userService.createCookie("refresh_token", response.getRefreshtoken(), 2592000, "/api/users/refresh/approved");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(new RefreshTokenResponseDTO("Giriş başarılı"));
        }
    }


}
