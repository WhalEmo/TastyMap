package com.beem.TastyMap.rag.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_ai_quotas")
public class UserAiQuotaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "daily_token_limit", nullable = false)
    private Integer dailyTokenLimit = 50000;

    // Bugün harcadığı token miktarı
    @Column(name = "used_tokens_today", nullable = false)
    private Integer usedTokensToday = 0;

    // Kullanıcının kayıt olduğundan beri toplam harcadığı maliyet (TL)
    @Column(name = "lifetime_spent_try", nullable = false, precision = 12, scale = 4)
    private BigDecimal lifetimeSpentTry = BigDecimal.ZERO;

    // Kullanıcının kayıt olduğundan beri toplam harcadığı token
    @Column(name = "lifetime_tokens", nullable = false)
    private Long lifetimeTokens = 0L;

    // Günlük sıfırlanma kontrol tarihi
    @Column(name = "last_reset_date", nullable = false)
    private LocalDate lastResetDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAiQuotaEntity() {}

    public UserAiQuotaEntity(Long userId, Integer dailyTokenLimit) {
        this.userId = userId;
        this.dailyTokenLimit = dailyTokenLimit;
        this.usedTokensToday = 0;
        this.lifetimeSpentTry = BigDecimal.ZERO;
        this.lifetimeTokens = 0L;
        this.lastResetDate = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Gün değiştiyse bugünkü harcanan token'ı sıfırlar
     */
    public boolean checkAndResetDailyUsage() {
        LocalDate today = LocalDate.now();
        if (!this.lastResetDate.isEqual(today)) {
            this.usedTokensToday = 0;
            this.lastResetDate = today;
            this.updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Yeni token harcamasını kotaya ve toplam metriklere yansıtır
     */
    public void recordConsumption(int tokens, BigDecimal costTry) {
        checkAndResetDailyUsage();
        this.usedTokensToday += tokens;
        this.lifetimeTokens += tokens;
        this.lifetimeSpentTry = this.lifetimeSpentTry.add(costTry);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasRemainingQuota() {
        checkAndResetDailyUsage();
        return this.usedTokensToday < this.dailyTokenLimit;
    }

    // Getter & Setter
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getDailyTokenLimit() { return dailyTokenLimit; }
    public void setDailyTokenLimit(Integer dailyTokenLimit) { this.dailyTokenLimit = dailyTokenLimit; }
    public Integer getUsedTokensToday() { return usedTokensToday; }
    public void setUsedTokensToday(Integer usedTokensToday) { this.usedTokensToday = usedTokensToday; }
    public BigDecimal getLifetimeSpentTry() { return lifetimeSpentTry; }
    public void setLifetimeSpentTry(BigDecimal lifetimeSpentTry) { this.lifetimeSpentTry = lifetimeSpentTry; }
    public Long getLifetimeTokens() { return lifetimeTokens; }
    public void setLifetimeTokens(Long lifetimeTokens) { this.lifetimeTokens = lifetimeTokens; }
    public LocalDate getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(LocalDate lastResetDate) { this.lastResetDate = lastResetDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}