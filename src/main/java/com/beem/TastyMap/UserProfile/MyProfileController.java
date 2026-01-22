package com.beem.TastyMap.UserProfile;

import com.beem.TastyMap.Security.RefreshTokenRequestDTO;
import com.beem.TastyMap.UserProfile.Subscribe.ProfileDTOresponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/myProfile")
public class MyProfileController {
    private final ProfileService profileService;

    public MyProfileController(ProfileService profileService) {
        this.profileService = profileService;
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
    public void logout(@RequestBody RefreshTokenRequestDTO dto, Authentication authentication) {
        Long userId=(Long)authentication.getPrincipal();
        profileService.logout(dto,userId);
    }

    @PostMapping("/update")
    private Map<String, String> updateProfile(
            @Valid @RequestBody UpdateProfileDTO req,
            Authentication authentication
    ){
        Long userId=(Long) authentication.getPrincipal();
        profileService.updateProfile(req,userId);

        return Map.of("message", "Profil Güncellendi");
    }

    @PostMapping("/changePassword")
    private Map<String,String> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            Authentication authentication
    ){
        Long userId=(Long)authentication.getPrincipal();
        profileService.changePassword(dto,userId);
        return Map.of("message","Şifre başarıyla değiştirildi!");
    }

    @GetMapping("/me")
    private ProfileDTOresponse getProfile(
            Authentication authentication
    ){
        Long myId=(Long)authentication.getPrincipal();
        return profileService.getProfile(myId,myId);
    }

}
