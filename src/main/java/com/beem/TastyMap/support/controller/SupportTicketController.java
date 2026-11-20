package com.beem.TastyMap.support.controller;

import com.beem.TastyMap.support.dto.CreateSupportTicketRequest;
import com.beem.TastyMap.support.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/support")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping("/tickets")
    public ResponseEntity<Void> createTicket(
            @Valid @RequestBody CreateSupportTicketRequest request,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        supportTicketService.createTicket(myId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}