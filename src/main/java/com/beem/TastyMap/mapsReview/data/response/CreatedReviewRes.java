package com.beem.TastyMap.mapsReview.data.response;

import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.beem.TastyMap.mapsReview.data.ScoreDto;

import java.util.List;

public class CreatedReviewRes {
    private Long reviewId;
    private Long placeId;
    private String author;
    private ReviewStatus status;
    private Long createdAt;
    private List<ScoreDto> scores;

    public CreatedReviewRes() {
    }

    public CreatedReviewRes(Long reviewId, Long placeId, String author, ReviewStatus status, Long createdAt, List<ScoreDto> scores) {
        this.reviewId = reviewId;
        this.placeId = placeId;
        this.author = author;
        this.status = status;
        this.createdAt = createdAt;
        this.scores = scores;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public List<ScoreDto> getScores() {
        return scores;
    }

    public void setScores(List<ScoreDto> scores) {
        this.scores = scores;
    }
}
