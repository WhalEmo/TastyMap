package com.beem.TastyMap.userRelated.profile;

import com.beem.TastyMap.registerLogin.ClientTypes;
import com.beem.TastyMap.registerLogin.UserService;
import com.beem.TastyMap.registerLogin.dto.UserResponseDTO;
import com.beem.TastyMap.security.refreshToken.RefreshTokenRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
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
            @RequestParam String deviceId,
            HttpServletResponse response,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        profileService.logout(deviceId, userId);

        ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        ResponseCookie deleteRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString());

        return ResponseEntity.ok().build();
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
