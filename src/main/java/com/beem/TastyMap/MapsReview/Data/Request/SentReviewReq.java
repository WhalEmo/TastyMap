package com.beem.TastyMap.MapsReview.Data.Request;

import java.util.List;

public class SentReviewReq {
    private Long userId;
    private Long parentId;
    private String content;
    private Long placeId;

    private List<ScoreRequest> scores;


    public SentReviewReq() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }

    public List<ScoreRequest> getScores() {
        return scores;
    }

    public void setScores(List<ScoreRequest> scores) {
        this.scores = scores;
    }
}
