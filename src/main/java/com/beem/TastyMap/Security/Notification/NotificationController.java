package com.beem.TastyMap.Security.Notification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
   private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    @PostMapping("/approve")
    public Map<String, String> approveDevice(@RequestParam Long notificationId) {
        notificationService.approve(notificationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cihaz onaylandı");
        return response;
    }
    @PostMapping("/rejected")
    public Map<String, String> rejectDevice(@RequestParam Long notificationId) {
        notificationService.reject(notificationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cihaz reddedildi");
        return response;
    }
}
