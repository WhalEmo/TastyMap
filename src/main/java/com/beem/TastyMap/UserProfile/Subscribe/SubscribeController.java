package com.beem.TastyMap.UserProfile.Subscribe;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscribe")
public class SubscribeController {
    private final SubscribeService subscribeService;

    public SubscribeController(SubscribeService subscribeService) {
        this.subscribeService = subscribeService;
    }

    @PostMapping("/{userId}")
    public void subscribe(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.subscribe(userId, myId);
    }

    @DeleteMapping("/unSubscribe/{userId}")
    public void unSubscribe(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.unSubscribe(userId, myId);
    }

    @DeleteMapping("/unSubscriber/{userId}")
    public void unSubscriber(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.unSubscriber(userId, myId);
    }
}
