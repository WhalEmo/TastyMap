package com.beem.TastyMap.user.profile.controller;

import com.beem.TastyMap.user.profile.dto.ProfileDTOresponse;
import com.beem.TastyMap.user.profile.service.OtherProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-profile")
public class UserProfileController {
    private final OtherProfileService otherProfileService;

    public UserProfileController(OtherProfileService otherProfileService) {
        this.otherProfileService = otherProfileService;
    }
    @GetMapping("/id/{userId}")
    public ProfileDTOresponse getProfileById(@PathVariable Long userId, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return otherProfileService.getUserProfileByUserId(userId, myId);
    }

    @GetMapping("/username/{username}")
    public ProfileDTOresponse getProfileByUsername(@PathVariable String username, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return otherProfileService.getUserProfileByUsername(username, myId);
    }

}
