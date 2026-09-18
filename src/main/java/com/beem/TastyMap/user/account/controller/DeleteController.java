package com.beem.TastyMap.user.account.controller;

import com.beem.TastyMap.user.account.dto.DeleteAccountRequest;
import com.beem.TastyMap.user.account.service.DeleteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/account")
public class DeleteController {

    private final DeleteService deleteService;

    public DeleteController(DeleteService deleteService) {
        this.deleteService = deleteService;
    }

    @DeleteMapping("/me/delete")
    public ResponseEntity<Void> deleteMyAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        deleteService.deleteAccount(myId, request);

        return ResponseEntity.ok().build();
    }
}