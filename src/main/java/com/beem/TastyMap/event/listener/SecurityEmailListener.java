package com.beem.TastyMap.event.listener;

import com.beem.TastyMap.event.model.SecurityEmailEvent;
import com.beem.TastyMap.security.verification.pendingRiskVerify.PendingService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
public class SecurityEmailListener {
    private final JavaMailSender javaMailSender;

    public SecurityEmailListener(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }
    @Value("${app.base-url}")
    private String baseURL;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityEvent(SecurityEmailEvent event) throws Exception {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("beemdevops@gmail.com");
            helper.setTo(event.getEmail());
            helper.setSubject("Güvenlik Uyarısı: Şüpheli Giriş Denemesi");

            String approveLink = baseURL+ "/auth/verifyLogin?token=" + event.getToken() + "&action=approve";
            String rejectLink = baseURL+ "/auth/verifyLogin?token="+ event.getToken() + "&action=reject";


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
        } catch (Exception e) {
          System.out.println("Mail gönderilemedi: "+ e);
        }
    }
}
