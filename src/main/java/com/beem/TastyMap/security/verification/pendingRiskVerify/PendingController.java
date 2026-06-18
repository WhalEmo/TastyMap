package com.beem.TastyMap.security.verification.pendingRiskVerify;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class PendingController {
    private final PendingService pendingService;

    public PendingController(PendingService pendingService) {
        this.pendingService = pendingService;
    }
    @GetMapping("/verifyLogin")
    public String verify(
            @RequestParam String token,
            @RequestParam String action
    ) {
        pendingService.verifyToken(token, action);

        boolean isApproved = "approve".equals(action);
        String statusText = isApproved ? "ONAYLANDI" : "REDDEDİLDİ";

        String extraMessage = isApproved
                ? "Artık güvenle uygulamanıza devam edebilirsiniz."
                : "Bu işlem size ait değilse, hesabınızın güvenliği için lütfen hemen şifrenizi değiştirin.";

        return """
        <div style="text-align: center; font-family: Arial, sans-serif; margin-top: 50px; padding: 20px;">
            <h1 style="color: %s;">İşlem %s!</h1>
            <p style="font-size: 18px; color: #333;">%s</p>
        </div>
        """.formatted(
                isApproved ? "#28a745" : "#dc3545",
                statusText,
                extraMessage
        );
    }
}
