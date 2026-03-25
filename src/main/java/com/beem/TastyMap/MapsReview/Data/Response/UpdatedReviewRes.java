package com.beem.TastyMap.MapsReview.Data.Response;

import com.beem.TastyMap.MapsReview.Data.ScoreDto;
import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;

import java.util.List;

public class UpdatedReviewRes {
    private Long reviewId;
    private Long placeId;
    private String author;
    private ReviewStatus status;
    private Long createdAt;
    private Long updateAt;
    private List<ScoreDto> scores;

    public UpdatedReviewRes() {
    }

    public UpdatedReviewRes(Long reviewId, Long placeId, String author, ReviewStatus status, Long createdAt, Long updateAt, List<ScoreDto> scores) {
        this.reviewId = reviewId;
        this.placeId = placeId;
        this.author = author;
        this.status = status;
        this.createdAt = createdAt;
        this.updateAt = updateAt;
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

    public Long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Long updateAt) {
        this.updateAt = updateAt;
    }
}
