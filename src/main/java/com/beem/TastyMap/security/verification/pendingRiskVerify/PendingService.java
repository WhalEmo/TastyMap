package com.beem.TastyMap.security.verification.pendingRiskVerify;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.websocket.LoginSecureEventService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PendingService {
    private final PendingRepo pendingRepo;
    private final NotificationRepo notificationRepo;
    private final LoginSecureEventService loginSecureEventService;
    private final JavaMailSender javaMailSender;

    public PendingService(PendingRepo pendingRepo, NotificationRepo notificationRepo, LoginSecureEventService loginSecureEventService, JavaMailSender javaMailSender) {
        this.pendingRepo = pendingRepo;
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
    public void verifyToken(String token,String action){
        PendingEntity pendingToken=pendingRepo.findByTokenWithUser(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Token geçersiz"));

        NotificationEntity notification = notificationRepo.findByUser_IdAndDeviceId(pendingToken.getUser().getId(), pendingToken.getDeviceId())
                .orElseThrow(() -> new CustomExceptions.InvalidException("Bildirim bulunamadı"));

        if(pendingToken.getExpiryDate().isBefore(LocalDateTime.now())){
            notification.setStatus(Status.REJECTED);
            throw new CustomExceptions.TokenExpiredException("Doğrulama linkinin süresi dolmuş. Lütfen yeni bir link isteyin.");
        }
        if (pendingToken.isUsed()) {
            throw new CustomExceptions.AlreadyVerifiedException("Zaten cevap verilmiş");
        }
        if ("approve".equals(action)) {
            notification.setStatus(Status.APPROVED);
            notification.setRead(true);
            try {
                loginSecureEventService.loginApproved(notification.getDeviceId());
            } catch (IOException e) {
                log.error("WebSocket hatası", e);
            }
        } else {
            notification.setStatus(Status.REJECTED);
            try {
                loginSecureEventService.loginRejected(notification.getDeviceId());
            } catch (IOException e) {
                log.error("WebSocket hatası", e);
            }
        }

        pendingToken.setUsed(true);
        pendingRepo.save(pendingToken);
        notificationRepo.save(notification);
    }
}
