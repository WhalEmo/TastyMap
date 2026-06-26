package com.beem.TastyMap.security.verification.pendingRiskVerify;
import com.beem.TastyMap.event.model.SecurityEmailModel;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.verification.emailVerify.EmailEntitiy;
import com.beem.TastyMap.websocket.LoginSecureEventService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PendingService {
    private final NotificationRepo notificationRepo;
    private final LoginSecureEventService loginSecureEventService;
    private final JavaMailSender javaMailSender;

    public PendingService( NotificationRepo notificationRepo, LoginSecureEventService loginSecureEventService, JavaMailSender javaMailSender) {
        this.notificationRepo = notificationRepo;
        this.loginSecureEventService = loginSecureEventService;
        this.javaMailSender = javaMailSender;
    }
    @Value("${app.base-url}")
    private String baseURL;

    public void sendSecurityAlertMail(String token, String email) throws Exception {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom("beemdevops@gmail.com");
        helper.setTo(email);
        helper.setSubject("Güvenlik Uyarısı: Şüpheli Giriş Denemesi");

        String approveLink = baseURL+ "/auth/verifyLogin?token=" + token + "&action=approve";
        String rejectLink = baseURL+ "/auth/verifyLogin?token="+ token + "&action=reject";


        //String verificationLinkW = "http://localhost:8081/#verify?token=" + token; //web
        //String verificationLinkA=baseURL+"/auth/verify?token="+token;

        String htmlBody = """
<div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
    <h2 style="color: #001970;">Hesabınızda Şüpheli Bir Giriş Tespit Ettik</h2>
    <p>Eğer bu giriş denemesi size ait değilse, hesabınızı korumak için lütfen hemen reddedin.</p>
    <p style="color: #d9534f; font-size: 14px; font-weight: bold; margin-bottom: 20px;">⚠️ Güvenliğiniz için bu işlem bağlantılarının geçerlilik süresi 10 dakikadır.</p>
    <div style="margin: 30px 0;">
        <a href="%s" style="background-color: #28a745; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">Girişi Onayla</a>
        <a href="%s" style="background-color: #dc3545; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; margin-left: 15px;">Girişi Reddet</a>
    </div>
    <p style="color: #666; font-size: 12px;">Bu işlem size ait değilse lütfen şifrenizi güncelleyin.</p>
</div>
""".formatted(approveLink, rejectLink);

        helper.setText(htmlBody, true);
        javaMailSender.send(mimeMessage);
    }

    @Transactional
    public void verifyToken(String token, String action) throws IOException {
        NotificationEntity notification = notificationRepo.findByTokenWithUser(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Token geçersiz"));

        if (notification.getExpiresAt().isBefore(LocalDateTime.now())) {
            notification.setStatus(Status.EXPIRED);
            notificationRepo.save(notification);
            throw new CustomExceptions.TokenExpiredException("Süre dolmuş.");
        }

        if (notification.isUsed()) {
            throw new CustomExceptions.AlreadyVerifiedException("Zaten cevap verilmiş.");
        }

        if ("approve".equals(action)) {
            notification.setStatus(Status.APPROVED);
            loginSecureEventService.loginApproved(notification.getDeviceId());
        } else {
            notification.setStatus(Status.REJECTED);
            loginSecureEventService.loginRejected(notification.getDeviceId());
        }

        notification.setUsed(true);
        notificationRepo.save(notification);
    }

    @Transactional
    public String resendSecurityAlertMail(String deviceId) throws Exception {
        NotificationEntity notification = notificationRepo.findFirstByDeviceIdAndIsUsedFalseOrderByCreatedAtDesc(deviceId)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Bekleyen aktif bir giriş onayı bulunamadı."));

        if (notification.getStatus() == Status.APPROVED || notification.getStatus() == Status.REJECTED) {
            throw new CustomExceptions.AlreadyVerifiedException("Bu işlem zaten sonuçlandırılmış.");
        }
        if (notification.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new CustomExceptions.AlreadyVerifiedException(
                    "Aktif bir doğrulama e-postanız zaten bulunmaktadır. Lütfen e-posta kutunuzu kontrol ediniz."
            );
        }

        String newToken = UUID.randomUUID().toString();
        notification.setToken(newToken);
        notification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        notification.setStatus(Status.PENDING);

        notificationRepo.save(notification);

        String userEmail = notification.getUser().getEmail();
        sendSecurityAlertMail(newToken, userEmail);
        return "Email gönderildi!";
    }


}
