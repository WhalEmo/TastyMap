package com.beem.TastyMap.rag.data.dto;

import com.beem.TastyMap.mapsReview.enums.ScoreType;

public record IndexedScoreDTO(
        ScoreType type,
        double score
) {}