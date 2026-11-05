package com.beem.TastyMap.rag.data.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserQuotaStatusDto(
        Long userId,
        int dailyLimit,
        int usedToday,
        int remainingTokens,
        boolean hasQuota,
        BigDecimal lifetimeSpentTry,
        LocalDate resetDate
) {}