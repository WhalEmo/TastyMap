package com.beem.TastyMap.rag.controller;

import com.beem.TastyMap.rag.data.dto.UserQuotaStatusDto;
import com.beem.TastyMap.rag.service.AiQuotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/quota")
public class AiQuotaController {

    private final AiQuotaService quotaService;

    public AiQuotaController(AiQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping
    public ResponseEntity<UserQuotaStatusDto> getQuotaStatus(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(quotaService.getUserQuotaStatus(userId));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Oturum açmış kullanıcı bulunamadı!");
        }
        if (authentication.getPrincipal() instanceof Long id) {
            return id;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Geçerli bir sayısal userId çözümlenemedi!");
        }
    }
}