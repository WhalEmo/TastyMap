package com.beem.TastyMap.visit.controller;

import com.beem.TastyMap.post.dto.PostAndVisitRequestDTO;
import com.beem.TastyMap.post.dto.PostResponseDTO;
import com.beem.TastyMap.visit.dto.VisitRequestDTO;
import com.beem.TastyMap.visit.service.VisitService;
import com.beem.TastyMap.visit.dto.VisitResponseDTO;
import jakarta.validation.Valid;
import kotlin.Unit;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService visitService;
    private final MessageSource messageSource;

    public VisitController(VisitService visitService, MessageSource messageSource) {
        this.visitService = visitService;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @PostMapping("/visit_and_post")
    public ResponseEntity<?> addVisitAndPost(
            @Valid @RequestBody PostAndVisitRequestDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        PostResponseDTO postResponse = visitService.processVisitAction(dto, myId);

        if (postResponse != null) {
            return ResponseEntity.ok(postResponse);
        }
        return ResponseEntity.ok(getMessage("visit.saved.success"));
    }
    @PostMapping("/save-visit")
    public ResponseEntity<VisitResponseDTO> createVisitAction(
            @Valid @RequestBody VisitRequestDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        VisitResponseDTO savedVisitDto = visitService.saveVisit(dto, myId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVisitDto);
    }

    @GetMapping("/get-visit")
    public Page<VisitResponseDTO> getMyVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return visitService.getVisits(myId, page, size);
    }

    @PatchMapping("/delete-visit/{visitId}")
    public void deleteVisit(
            @PathVariable Long visitId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        visitService.deleteVisit(visitId, myId);
    }
}