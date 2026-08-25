package com.beem.TastyMap.mapsReview.data.request;

import com.beem.TastyMap.mapsReview.data.ScoreDto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SentReviewReq {
    private Long parentId;
    private String content;
    private String placeId;

    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private Double mainRating;

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


    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public List<ScoreDto> getScores() {
        return scores;
    }

    public void setScores(List<ScoreDto> scores) {
        this.scores = scores;
    }

    public Double getMainRating() {
        return mainRating;
    }

    public void setMainRating(Double mainRating) {
        this.mainRating = mainRating;
    }
}
