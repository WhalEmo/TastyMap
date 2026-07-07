package com.beem.TastyMap.security.verification.forgotPassword;

import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
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
    public ResponseEntity<String> requestResetPassword(@RequestBody CommonRequestDTO dto) {
        passwordService.forgotPassword(dto);
        return ResponseEntity.ok("Şifre sıfırlama bağlantısı e-posta adresinize iletilmiştir. Bağlantı güvenliğiniz için 5 dakika geçerlidir, lütfen e-posta kutunuzu kontrol ediniz.");
    }


    @GetMapping("/resetPassword/validate")
    public void validateToken(@RequestParam String token){
        passwordService.validateAndGetToken(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordDTO request
    ) {
        String message=passwordService.newPassword(request);
        return ResponseEntity.ok(message);
    }

}
