package com.beem.TastyMap.stats.data.response;

import java.util.List;
import java.util.Map;

public record PlaceStatsDto(
        Double overallRating,
        Integer totalReviewCount,
        Map<Integer, Integer> starDistribution,
        List<ScoreMetricDto> criteriaMetrics,
        List<String> highlights
) {}