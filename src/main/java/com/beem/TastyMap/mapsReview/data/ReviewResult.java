package com.beem.TastyMap.mapsReview.data;

import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResult(
        Long id,
        Long userId,
        String name,
        String userProfile,
        double rating,
        String content,
        ReviewSource source,
        Long parentId,
        Integer likeCount,
        Long createdAt,
        Long updateAt,
        List<ScoreDto> scores
) {
    public ReviewResult {
        if (scores == null) {
            scores = List.of();
        }
    }
}