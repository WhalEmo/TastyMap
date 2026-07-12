package com.beem.TastyMap.security.verification.forgotPassword;

import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PasswordController {
    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }


    @PostMapping("/forgotPassword")
    public ResponseEntity<PasswordResetResponse>requestResetPassword(@RequestBody CommonRequestDTO dto) {
        PasswordResetResponse response= passwordService.forgotPassword(dto);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/resetPassword/validate")
    public void validateToken(@RequestParam String token){
        passwordService.validateAndGetToken(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordDTO request
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
