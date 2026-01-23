package com.beem.TastyMap.UserProfile.Subscribe;

import org.springframework.data.domain.Page;
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

    //benimabone oldukarlım
    @GetMapping("/getSubscribe/{userId}")
    public Page<SubscribeDTO> getUserSubscribes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return subscribeService.getUserSubscribes(userId, myId, page, size);
    }

    //bana abone olanlar
    @GetMapping("/getSubscribers/{userId}")
    public Page<SubscribeDTO> getUserSubscribers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return subscribeService.getUserSubscribers(userId, myId, page, size);
    }


}
