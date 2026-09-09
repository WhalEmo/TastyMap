package com.beem.TastyMap.userRelated.socialnotifications;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/socialnotifications")
public class SocialNotificationController {

    private final SocialNotificationService notificationService;

    public SocialNotificationController(SocialNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<SocialNotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        Page<SocialNotificationDTO> notifications = notificationService.getUserNotifications(myId, page, size);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/read")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        notificationService.markNotificationAsRead(myId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/has-unread")
    public ResponseEntity<Map<String, Boolean>> checkHasUnread(Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        boolean hasUnread = notificationService.checkHasUnread(myId);

        return ResponseEntity.ok(Map.of("hasUnread", hasUnread));
    }
}
