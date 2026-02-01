package com.beem.TastyMap.MapsReview.Data.Request;

import com.beem.TastyMap.MapsReview.Data.ScoreDto;

import java.util.List;

public class UpdateReviewReq {
    private Long reviewId;
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
}
