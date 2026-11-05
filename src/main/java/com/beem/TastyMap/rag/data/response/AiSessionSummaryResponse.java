package com.beem.TastyMap.rag.data.response;

import java.time.LocalDateTime;

public record AiSessionSummaryResponse(
        String sessionId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}