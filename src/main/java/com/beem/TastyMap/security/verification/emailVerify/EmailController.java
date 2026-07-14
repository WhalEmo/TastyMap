package com.beem.TastyMap.security.verification.emailVerify;

import com.beem.TastyMap.security.verification.common.CommonRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    @GetMapping("/verify")
    public ResponseEntity<Map<String,String>>verify(@RequestParam String token) throws IOException {
        System.out.println("VERIFY ENDPOINT ÇALIŞTI");
        System.out.println("Token = " + token);
        String result=emailService.verifyEmail(token);
        Map<String,String>response=new HashMap<>();
        response.put("message",result);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/resendMail")
    public ResponseEntity<Long> resendMail(
            @RequestBody CommonRequestDTO dto
    ) {
        long userId = emailService.resendVerification(dto);
        return ResponseEntity.ok(userId);
    }

    @GetMapping("/check-used")
    public ResponseEntity<Boolean> isEmailUsedByDevice(@RequestParam Long userId) {
        boolean isUsed = emailService.isUsedEmail(userId);
        return ResponseEntity.ok(isUsed);
    }
}
