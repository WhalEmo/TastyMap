package com.beem.TastyMap.MapsReview.Data.Request;

import com.beem.TastyMap.MapsReview.Data.ScoreDto;

import java.util.List;

public class SentReviewReq {
    private Long parentId;
    private String content;
    private Long placeId;

    private List<ScoreDto> scores;


    public SentReviewReq() {
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

    public List<ScoreDto> getScores() {
        return scores;
    }

    public void setScores(List<ScoreDto> scores) {
        this.scores = scores;
    }
}
