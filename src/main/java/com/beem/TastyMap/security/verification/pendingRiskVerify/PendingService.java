package com.beem.TastyMap.security.verification.pendingRiskVerify;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.notification.NotificationEntity;
import com.beem.TastyMap.notification.NotificationRepo;
import com.beem.TastyMap.notification.Status;
import com.beem.TastyMap.registerLogin.LoginStatus;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PendingService {
    private final PendingRepo pendingRepo;
    private final NotificationRepo notificationRepo;
    private final JavaMailSender javaMailSender;

    public PendingService(PendingRepo pendingRepo, NotificationRepo notificationRepo, JavaMailSender javaMailSender) {
        this.pendingRepo = pendingRepo;
        this.notificationRepo = notificationRepo;
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

        String approveLink = "https://tastymap.com/api/auth/approve?token=" + token + "&action=approve";
        String rejectLink = "https://tastymap.com/api/auth/reject?token=" + token + "&action=reject";


        String verificationLinkW = "http://localhost:8081/#verify?token=" + token; //web
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
    public boolean verifyToken(String token,String action,Long userId,String deviceId){
        PendingEntity pendingToken=pendingRepo.findByToken(token)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Token geçersiz"));

        NotificationEntity notificationOpt = notificationRepo.findByUser_IdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new CustomExceptions.InvalidException("Bildirim bulunamadı"));

        if(pendingToken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new CustomExceptions.TokenExpiredException("Doğrulama linkinin süresi dolmuş. Lütfen yeni bir link isteyin.");
        }
        if (pendingToken.isUsed()) {
            throw new CustomExceptions.AlreadyVerifiedException("Zaten cevap verilmiş");
        }
        boolean success = false;
        if ("approve".equals(action)) {
            notificationOpt.setRead(true);
            notificationOpt.setStatus(Status.APPROVED);
            success = refreshservice.refreshApproved(pendingToken.getUserId(), pendingToken.getSessionId());
        } else {

        }

        // 5. Token'ı "kullanıldı" olarak işaretle
        pendingToken.setUsed(true);
        pendingRepo.save(pendingToken);

        return success;
    }
}
