package com.beem.TastyMap.security.verification.forgotpassword.controller;

import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import com.beem.TastyMap.security.verification.forgotpassword.dto.PasswordResetResponseDTO;
import com.beem.TastyMap.security.verification.forgotpassword.service.PasswordService;
import com.beem.TastyMap.security.verification.forgotpassword.dto.PasswordResetRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class PasswordController {
    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }


    @PostMapping("/forgotPassword")
    public ResponseEntity<PasswordResetResponseDTO>requestResetPassword(@RequestBody CommonRequestDTO dto) {
        PasswordResetResponseDTO response= passwordService.forgotPassword(dto);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/resetPassword/validate")
    public void validateToken(@RequestParam String token){
        passwordService.validateAndGetToken(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(
            @RequestBody PasswordResetRequestDTO request
    ) throws IOException {

        String response = passwordService.newPassword(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-password")
    public ResponseEntity<Boolean> isEmailUsedByDevice(@RequestParam Long userId) {
        boolean isUsed = passwordService.isUsedPassword(userId);
        return ResponseEntity.ok(isUsed);
    }

}
