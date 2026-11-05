package com.beem.TastyMap.rag.controller;

import com.beem.TastyMap.rag.data.response.AiChatMessageResponse;
import com.beem.TastyMap.rag.data.response.AiSessionSummaryResponse;
import com.beem.TastyMap.rag.service.AiChatHistoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/sessions")
public class AiChatHistoryController {

    private final AiChatHistoryService chatHistoryService;

    public AiChatHistoryController(AiChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * Sol panel için kullanıcının geçmiş sohbet oturumlarını sayfalı (Slice) döner.
     * Örnek istek: GET /api/v1/ai/sessions?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Slice<AiSessionSummaryResponse>> getSessions(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(chatHistoryService.getUserSessions(userId, pageable));
    }

    /**
     * Tıklanan bir oturumun tüm mesajlarını ve canlı zenginleştirilmiş (hydrated) mekan kartlarını getirir.
     * Örnek istek: GET /api/v1/ai/sessions/{sessionId}/messages
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<AiChatMessageResponse>> getSessionMessages(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(chatHistoryService.getSessionMessages(sessionId, userId));
    }

    /**
     * Oturumu ve ona bağlı mesajları veritabanından tamamen siler.
     * Örnek istek: DELETE /api/v1/ai/sessions/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        chatHistoryService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Authentication nesnesinden güvenli şekilde userId çözümler.
     */
    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Oturum açmış kullanıcı bulunamadı!");
        }

        Object principal = authentication.getPrincipal();

        // Eğer principal doğrudan String (sub/id) veya Long tutuyorsa:
        if (principal instanceof Long id) {
            return id;
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // Eğer authentication.getName() email veya username ise kendi UserDetails modelinin getId() metodunu çağır
            throw new IllegalStateException("Authentication context üzerinden geçerli bir sayısal userId çözümlenemedi!");
        }
    }
}