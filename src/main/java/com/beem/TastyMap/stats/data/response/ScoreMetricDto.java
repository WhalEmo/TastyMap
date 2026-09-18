package com.beem.TastyMap.stats.data.response;


import com.beem.TastyMap.mapsReview.enums.ScoreType;

public record ScoreMetricDto(
        ScoreType type,
        String label,
        Double averageScore,
        Integer count
) {}