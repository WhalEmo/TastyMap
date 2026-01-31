package com.beem.TastyMap.Security.Verification.EmailVerify;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Map<String,String>>verify(@RequestParam String token){
        String result=emailService.verifyEmail(token);
        Map<String,String>response=new HashMap<>();
        response.put("message",result);
        return ResponseEntity.ok(response);
    }
}
