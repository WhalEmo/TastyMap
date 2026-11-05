package com.beem.TastyMap.rag.data.response;

import java.math.BigDecimal;

public record QuotaFeedbackPayload(
        int tokensUsedThisRequest,
        int tokensUsedToday,
        int dailyTokenLimit,
        double remainingPercentage,
        BigDecimal costTry
) {}
