package com.beem.TastyMap.Security.Verification.ForgotPassword;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public void requestResetPassword(@RequestParam String email){
        passwordService.forgotPassword(email);
    }

    @GetMapping("/resetPassword/validate")
    public void validateToken(@RequestParam String token){
        passwordService.validateToken(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam String token,
            @RequestBody ResetPasswordDTO request
    ) {
        String mesaj=passwordService.newPassword(token,request);

        Map<String,String>response=new HashMap<>();
        response.put("message",mesaj);

        return ResponseEntity.ok(response);
    }

}
