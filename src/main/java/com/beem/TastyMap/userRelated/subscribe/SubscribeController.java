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
    public ResponseEntity<SubscribeDTO> subscribe(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        SubscribeDTO response = subscribeService.subscribe(userId, myId);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/accept/{requesterId}")
    public ResponseEntity<Void> acceptSubscribeRequest(
            @PathVariable Long requesterId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.acceptSubscribeRequest(requesterId, myId);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/reject/{requesterId}")
    public ResponseEntity<Void> rejectSubscribeRequest(
            @PathVariable Long requesterId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.rejectSubscribeRequest(requesterId, myId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unSubscribe/{userId}")
    public ResponseEntity<Void> unSubscribe(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.unSubscribe(userId, myId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unSubscriber/{userId}")
    public ResponseEntity<Void> unSubscriber(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        subscribeService.unSubscriber(userId, myId);
        return ResponseEntity.ok().build();
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
