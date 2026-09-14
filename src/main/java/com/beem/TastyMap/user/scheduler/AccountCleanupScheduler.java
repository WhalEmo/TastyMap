package com.beem.TastyMap.user.scheduler;

import com.beem.TastyMap.user.account.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupScheduler {

    private final UserRepo userRepo;

    // Her gece saat 03:00'da çalışır
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanUpDeletedAccounts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        try {
            int deletedCount = userRepo.hardDeleteExpiredAccounts(threshold);

            if (deletedCount > 0) {
                log.info("Zamanlanmış görev başarılı: {} adet süresi dolmuş hesap kalıcı olarak silindi.", deletedCount);
            }
        } catch (Exception e) {
            log.error("Süresi dolmuş hesapları kalıcı olarak silme işlemi sırasında hata oluştu: ", e);
        }
    }
}