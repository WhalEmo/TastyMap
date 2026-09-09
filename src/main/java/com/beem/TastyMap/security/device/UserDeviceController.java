package com.beem.TastyMap.security.device;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    public UserDeviceController(UserDeviceService userDeviceService) {
        this.userDeviceService = userDeviceService;
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @Valid @RequestBody FcmTokenUpdateRequest request,
            Authentication authentication
    ) {

        Long myId = (Long) authentication.getPrincipal();
        userDeviceService.updateFcmToken(
                myId,
                request.getDeviceId(),
                request.getFcmToken()
        );

        return ResponseEntity.ok().build();
    }
}