package com.beem.TastyMap.rag.data.response;

import java.io.Serializable;

public record RecommendationRes(
        Long userId,
        String aiMessage,
        boolean hasMore,
        int remainingCount
) implements Serializable {
}