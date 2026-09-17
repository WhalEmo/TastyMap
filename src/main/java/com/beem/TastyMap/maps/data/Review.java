package com.beem.TastyMap.maps.data;

import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Review(
        @JsonProperty("review_id") Long reviewId,
        ReviewSource source,
        @JsonProperty("author_name") String authorName,
        Double rating,
        String text,
        Long time,
        List<ScoreDto> scores
) {
    public Review {
        if (scores == null) {
            scores = List.of();
        }
    }
}