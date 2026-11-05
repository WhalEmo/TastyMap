package com.beem.TastyMap.rag.data.dto;

import java.util.List;

public record IndexedReviewDTO(
        Long reviewId,
        String text,
        List<IndexedScoreDTO> scores
) {}