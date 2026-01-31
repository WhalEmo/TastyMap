package com.beem.TastyMap.MapsReview.Data.Request;

import java.util.List;

public class UpdateReviewReq {
    private Long reviewId;
    private String content;

    private List<ScoreRequest> scores;

    public UpdateReviewReq() {
    }

    public List<ScoreRequest> getScores() {
        return scores;
    }

    public void setScores(List<ScoreRequest> scores) {
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
