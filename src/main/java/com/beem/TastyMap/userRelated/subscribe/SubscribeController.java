package com.beem.TastyMap.userRelated.subscribe;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SubscribeActionResult> subscribe(@PathVariable Long userId, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.subscribe(userId, myId));
    }

    @PostMapping("/accept/{requesterId}")
    public ResponseEntity<SubscribeActionResult> acceptRequest(@PathVariable Long requesterId, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.acceptSubscribeRequest(requesterId, myId));
    }

    @PostMapping("/reject/{requesterId}")
    public ResponseEntity<SubscribeActionResult> rejectRequest(@PathVariable Long requesterId, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.rejectSubscribeRequest(requesterId, myId));
    }

    @DeleteMapping("/unSubscribe/{userId}")
    public ResponseEntity<SubscribeActionResult> unSubscribe(@PathVariable Long userId, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.unSubscribe(userId, myId));
    }

    @DeleteMapping("/unSubscriber/{userId}")
    public ResponseEntity<SubscribeActionResult> unSubscriber(@PathVariable Long userId, Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.unSubscriber(userId, myId));
    }

    //benimabone oldukarlım
    @GetMapping("/getSubscribe/{userId}")
    public ResponseEntity<Page<SubscribeDTO>> getUserSubscribes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.getUserSubscribes(userId, myId, page, size));
    }

    // Bir kullanıcıya abone olanlar
    @GetMapping("/getSubscribers/{userId}")
    public ResponseEntity<Page<SubscribeDTO>> getUserSubscribers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.getUserSubscribers(userId, myId, page, size));
    }

    // Bana Gelen Onay Bekleyen Abonelik İstekleri Listesi
    @GetMapping("/requests")
    public ResponseEntity<Page<SubscribeDTO>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(subscribeService.getPendingRequests(myId, page, size));
    }


}
