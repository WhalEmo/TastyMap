package com.beem.TastyMap.UserProfile;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/userProfile")
public class UserProfileController {
    private final ProfileService profileService;

    public UserProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile/{userId}")
    public ProfileDTOresponse getUserProfile(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId=(Long)authentication.getPrincipal();
        return profileService.getProfile(userId,myId);
    }
}
