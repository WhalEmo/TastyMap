package com.beem.TastyMap.userRelated.profile;

import com.beem.TastyMap.registerLogin.ClientTypes;
import com.beem.TastyMap.registerLogin.UserService;
import com.beem.TastyMap.registerLogin.dto.UserResponseDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRequestDTO;
import jakarta.validation.Valid;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/myProfile")
public class MyProfileController {
    private final ProfileService profileService;
    private final UserService userService;

    public MyProfileController(ProfileService profileService, UserService userService) {
        this.profileService = profileService;
        this.userService = userService;
    }

    @GetMapping("/active")
    public Map<String, Object>getActiveDevices(Authentication authentication){
        Long userId=(Long)authentication.getPrincipal();
        List<ActiveDeviceDTO> devices=profileService.getActiveDevices(userId);
        Long count=profileService.getActiveDeviceCount(userId);
        Map<String,Object> response=new HashMap<>();
        response.put("activeDeviceCount", count);
        response.put("devices", devices);
        return response;
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody RefreshTokenRequestDTO dto,
            @RequestHeader(value = "X-Client-Type", defaultValue = ClientTypes.WEB) String clientType,
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            Authentication authentication
    ) {
        Long userId=(Long)authentication.getPrincipal();

        String refreshToken = ClientTypes.MOBILE.equalsIgnoreCase(clientType) && dto != null
                ? dto.getRefreshToken()
                : cookieRefreshToken;

        profileService.logout(new RefreshTokenRequestDTO(refreshToken, dto.getDeviceId()), userId);

        if (ClientTypes.MOBILE.equalsIgnoreCase(clientType)) {
            return ResponseEntity.ok().build();
        } else {
            ResponseCookie clearAccessCookie = userService.createCookie("access_token", "", 0, "/");
            ResponseCookie clearRefreshCookie = userService.createCookie("refresh_token", "", 0, "/api/users/refresh");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                    .build();
        }
    }

    @PostMapping("/update")
    public Map<String, String> updateProfile(
            @Valid @RequestBody UpdateProfileDTO req,
            Authentication authentication
    ){
        Long userId=(Long) authentication.getPrincipal();
        profileService.updateProfile(req,userId);

        return Map.of("message", "Profil Güncellendi");
    }

    @PostMapping("/changePassword")
    public Map<String,String> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            Authentication authentication
    ){
        Long userId=(Long)authentication.getPrincipal();
        profileService.changePassword(dto,userId);
        return Map.of("message","Şifre başarıyla değiştirildi!");
    }

    @GetMapping("/meProfile")
    public ProfileDTOresponse getProfile(
            Authentication authentication
    ){
        Long myId=(Long)authentication.getPrincipal();
        return profileService.getProfile(myId,myId);
    }

    @GetMapping("/me")
    public UserResponseDTO getMe(
            Authentication authentication
    ){
        Long myId=(Long)authentication.getPrincipal();
        return profileService.getMe(myId);
    }

}
