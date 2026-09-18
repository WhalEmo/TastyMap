package com.beem.TastyMap.mapsReview.data.request;

import com.beem.TastyMap.mapsReview.data.ScoreDto;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateReviewReq {
    @NotNull
    private Long reviewId;

    @NotNull
    private Double mainRating;

    private String content;

    private List<ScoreDto> scores;

    public UpdateReviewReq() {
    }

    public List<ScoreDto> getScores() {
        return scores;
    }

    public void setScores(List<ScoreDto> scores) {
        this.scores = scores;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Double getMainRating() {
        return mainRating;
    }

    public void setMainRating(Double mainRating) {
        this.mainRating = mainRating;
    }
}
