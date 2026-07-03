package com.beem.TastyMap.security.verification.pendingRiskVerify;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.NotificationResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class PendingController {
    private final PendingService pendingService;

    public PendingController(PendingService pendingService) {
        this.pendingService = pendingService;
    }

    @GetMapping(value = "/verifyLogin", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam String token,
            @RequestParam String action
    ) throws IOException {
        pendingService.verifyToken(token, action);

        boolean isApproved = "approve".equals(action);
        String statusText = isApproved ? "ONAYLANDI" : "REDDEDİLDİ";
        String color = isApproved ? "#28a745" : "#dc3545";
        String extraMessage = isApproved
                ? "Artık güvenle uygulamanıza devam edebilirsiniz."
                : "Bu işlem size ait değilse, hesabınızın güvenliği için lütfen hemen şifrenizi değiştirin.";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>TastyMap - İşlem Durumu</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                        .card { background: white; padding: 40px; border-radius: 15px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); text-align: center; max-width: 400px; }
                        h1 { color: %s; margin-bottom: 20px; }
                        p { color: #555; line-height: 1.6; }
                        .icon { font-size: 50px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">%s</div>
                        <h1>İşlem %s!</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(
                color,
                isApproved ? "✅" : "❌",
                statusText,
                extraMessage
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .body(html);
    }

    @PostMapping("/resend-security-mail")
    public ResponseEntity<String> resendSecurityMail(
            @RequestParam String deviceId,
            @RequestParam String fingerPrintHash

    ) {
        try {
            String result = pendingService.resendSecurityAlertMail(deviceId,fingerPrintHash);
            return ResponseEntity.ok(result);

        } catch (CustomExceptions.AlreadyVerifiedException e) {
            return ResponseEntity.ok(e.getMessage());

        } catch (CustomExceptions.InvalidException e) {
            return ResponseEntity.ok(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.ok(e.getMessage());
        }


    }

    @GetMapping("/is-used")
    public ResponseEntity<NotificationResponse> isUsedNotification(
            @RequestParam String deviceId,
            @RequestParam String fingerPrintHash
    ) {
        return ResponseEntity.ok(
                pendingService.isUsedNotification(deviceId,fingerPrintHash)
        );
    }
}


