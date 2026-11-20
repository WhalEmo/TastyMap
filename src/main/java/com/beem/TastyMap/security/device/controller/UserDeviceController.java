package com.beem.TastyMap.security.device.controller;

import com.beem.TastyMap.security.device.dto.FcmTokenUpdateRequest;
import com.beem.TastyMap.security.device.dto.NotificationPreferenceRequest;
import com.beem.TastyMap.security.device.service.UserDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/notifications/preferences")
    public ResponseEntity<Void> updateNotificationPreference(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        userDeviceService.updateNotificationPreference(
                myId,
                request.getDeviceId(),
                request.getIsEnabled()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications/preferences")
    public ResponseEntity<Boolean> getNotificationPreference(
            @RequestParam String deviceId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        boolean isEnabled = userDeviceService.getNotificationPreference(myId, deviceId);
        return ResponseEntity.ok(isEnabled);
    }

}