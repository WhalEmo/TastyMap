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
        return ResponseEntity.ok("Eğer e-posta adresi sistemimizde kayıtlıysa, şifre sıfırlama bağlantısı gönderilecektir.");
    }


    @GetMapping("/resetPassword/validate")
    public void validateToken(@RequestParam String token){
        passwordService.validateAndGetToken(token);
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
