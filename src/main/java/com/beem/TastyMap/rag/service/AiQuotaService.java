package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.dto.UserQuotaStatusDto;
import com.beem.TastyMap.rag.data.enums.AiProvider;
import com.beem.TastyMap.rag.entity.AiTokenUsageEntity;
import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import com.beem.TastyMap.rag.entity.ChatSessionEntity;
import com.beem.TastyMap.rag.entity.UserAiQuotaEntity;
import com.beem.TastyMap.rag.repository.AiTokenUsageRepository;
import com.beem.TastyMap.rag.repository.ChatMessageRepository;
import com.beem.TastyMap.rag.repository.ChatSessionRepository;
import com.beem.TastyMap.rag.repository.UserAiQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);

    private final UserAiQuotaRepository quotaRepository;
    private final AiTokenUsageRepository usageRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    @Value("${tastymap.ai.cost.usd-try-rate:50.0}")
    private double usdTryRate;

    @Value("${tastymap.ai.default-daily-quota:50000}")
    private int defaultDailyQuota;

    public AiQuotaService(UserAiQuotaRepository quotaRepository,
                          AiTokenUsageRepository usageRepository,
                          ChatSessionRepository sessionRepository,
                          ChatMessageRepository messageRepository) {
        this.quotaRepository = quotaRepository;
        this.usageRepository = usageRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 1. İstek Öncesi Kapı Kontrolü (Hafif ve Hızlı Okuma).
     * Kullanıcının bugünkü kullanım kotasını aşmadığını teyit eder.
     */
    @Transactional
    public boolean hasAvailableQuota(Long userId) {
        if (userId == null) {
            return false;
        }

        UserAiQuotaEntity quota = quotaRepository.findById(userId)
                .orElseGet(() -> createDefaultQuota(userId));

        // Gün değiştiyse bugünkü harcamayı sıfırla
        quota.checkAndResetDailyUsage();
        quotaRepository.save(quota);

        boolean available = quota.hasRemainingQuota();
        if (!available) {
            log.warn("⚠️ Kullanıcı {} için günlük AI token kotası tükendi! (Limit: {}, Kullanılan: {})",
                    userId, quota.getDailyTokenLimit(), quota.getUsedTokensToday());
        }
        return available;
    }

    /**
     * Kullanıcının güncel kota durumunu mobil arayüze döner.
     */
    @Transactional(readOnly = true)
    public UserQuotaStatusDto getUserQuotaStatus(Long userId) {
        UserAiQuotaEntity quota = quotaRepository.findById(userId)
                .orElseGet(() -> new UserAiQuotaEntity(userId, defaultDailyQuota));

        int remaining = Math.max(0, quota.getDailyTokenLimit() - quota.getUsedTokensToday());

        return new UserQuotaStatusDto(
                userId,
                quota.getDailyTokenLimit(),
                quota.getUsedTokensToday(),
                remaining,
                remaining > 0,
                quota.getLifetimeSpentTry(),
                quota.getLastResetDate()
        );
    }

    /**
     * 2. İstek Sonrası Asenkron Muhasebe ve Kota Düşümü.
     * Ana akışı ve WebSocket/SSE yanıtını geciktirmemek için ayrı bir thread havuzunda koşturulur.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUsageAndDeductQuota(
            Long userId,
            String sessionId,
            Long messageId,
            AiProvider provider,
            String modelName,
            int promptTokens,
            int completionTokens,
            long durationMs
    ) {
        try {
            int totalTokens = promptTokens + completionTokens;

            // Maliyet hesaplamaları
            BigDecimal costUsd = calculateUsdCost(modelName, promptTokens, completionTokens);
            BigDecimal costTry = costUsd.multiply(BigDecimal.valueOf(usdTryRate))
                    .setScale(6, RoundingMode.HALF_UP);

            ChatSessionEntity session = (sessionId != null)
                    ? sessionRepository.getReferenceById(sessionId)
                    : null;

            ChatMessageEntity message = (messageId != null)
                    ? messageRepository.getReferenceById(messageId)
                    : null;

            // 1. Atomik Token Logunu Veritabanına Yaz
            AiTokenUsageEntity usageRecord = new AiTokenUsageEntity(
                    userId,
                    session,
                    message,
                    provider,
                    modelName,
                    promptTokens,
                    completionTokens,
                    costUsd,
                    costTry,
                    durationMs
            );
            usageRepository.save(usageRecord);

            // 2. Kullanıcı Kotasını Güncelle (Pessimistic Lock ile güvenli düşüm)
            quotaRepository.findByUserIdForUpdate(userId).ifPresent(quota -> {
                quota.recordConsumption(totalTokens, costTry);
                quotaRepository.save(quota);
            });

            // 3. Oturumun Sayaçlarını Güncelle (Varsa)
            if (session != null) {
                sessionRepository.incrementSessionTotals(sessionId, totalTokens, costTry);
            }

            log.info("📊 [Quota & Cost Engine] Başarıyla İşlendi -> Kullanıcı: {} | Sağlayıcı: {} ({}) | Token: [In: {}, Out: {}, Tot: {}] | Süre: {} ms | Maliyet: ${} (~{} TL)",
                    userId, provider, modelName, promptTokens, completionTokens, totalTokens, durationMs, costUsd.toPlainString(), costTry.toPlainString());

        } catch (Exception e) {
            log.error("❌ Token kotası düşülürken ve harcama kaydedilirken kritik hata: {}", e.getMessage(), e);
        }
    }

    /**
     * 1 Milyon Token birim fiyatlarına göre hassas USD maliyeti çıkarır.
     */
    private BigDecimal calculateUsdCost(String modelName, int promptTokens, int completionTokens) {
        if (modelName == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal promptPricePerMillion;
        BigDecimal completionPricePerMillion;

        String lowerModel = modelName.toLowerCase();

        if (lowerModel.contains("qwen3.8-flash") || lowerModel.contains("deepseek-v4")) {
            // EVREN LLM / Flash Modeller (Tahmini ortalama 1M girdi/çıktı)
            promptPricePerMillion = new BigDecimal("0.15");
            completionPricePerMillion = new BigDecimal("0.30");
        } else if (lowerModel.contains("qwen3.8-27b")) {
            // Groq Qwen 27B
            promptPricePerMillion = new BigDecimal("0.40");
            completionPricePerMillion = new BigDecimal("0.80");
        } else if (lowerModel.contains("llama-3.3-70b") || lowerModel.contains("llama3.3")) {
            // 70B Büyük Modeller
            promptPricePerMillion = new BigDecimal("0.60");
            completionPricePerMillion = new BigDecimal("1.20");
        } else if (lowerModel.contains("llama3.1:8b") || lowerModel.contains("ollama")) {
            // Yerel Ollama modellerinde şirket dışına API faturası çıkmaz
            promptPricePerMillion = BigDecimal.ZERO;
            completionPricePerMillion = BigDecimal.ZERO;
        } else {
            // Tanımlanmamış modeller için varsayılan güvenli marj
            promptPricePerMillion = new BigDecimal("0.20");
            completionPricePerMillion = new BigDecimal("0.50");
        }

        BigDecimal promptCost = promptPricePerMillion
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal completionCost = completionPricePerMillion
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return promptCost.add(completionCost);
    }

    private UserAiQuotaEntity createDefaultQuota(Long userId) {
        UserAiQuotaEntity quota = new UserAiQuotaEntity(userId, defaultDailyQuota);
        return quotaRepository.save(quota);
    }
}