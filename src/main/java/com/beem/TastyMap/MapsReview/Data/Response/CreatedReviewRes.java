package com.beem.TastyMap.MapsReview.Data.Response;

import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;

public class CreatedReviewRes {
    private Long reviewId;
    private Long placeId;
    private String author;
    private ReviewStatus status;
    private Long createdAt;

    public CreatedReviewRes() {
    }

    public CreatedReviewRes(Long reviewId, Long placeId, String author, ReviewStatus status, Long createdAt) {
        this.reviewId = reviewId;
        this.placeId = placeId;
        this.author = author;
        this.status = status;
        this.createdAt = createdAt;
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
}
