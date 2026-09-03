package com.beem.TastyMap.userRelated.profile;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/userProfile")
public class UserProfileController {
    private final OtherProfileService otherProfileService;

    public UserProfileController(OtherProfileService otherProfileService) {
        this.otherProfileService = otherProfileService;
    }

    @GetMapping("/profile/{userId}")
    public ProfileDTOresponse getUserProfile(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId=(Long)authentication.getPrincipal();
        return otherProfileService.getUserProfile(userId,myId);
    }
}
