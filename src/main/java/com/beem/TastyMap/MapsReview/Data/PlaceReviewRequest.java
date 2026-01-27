package com.beem.TastyMap.MapsReview.Data;

import java.util.List;

public class PlaceReviewRequest {
    private Long userId;
    private Long parentId;
    private String content;
    private Long placeId;

    private List<ScoreRequest> scores;


    public PlaceReviewRequest() {
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
