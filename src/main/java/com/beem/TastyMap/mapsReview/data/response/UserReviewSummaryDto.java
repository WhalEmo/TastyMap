package com.beem.TastyMap.mapsReview.data.response;

import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;

import java.util.Collections;
import java.util.List;

public class UserReviewSummaryDto {
    private Long reviewId;
    private String userName;
    private Double rating;
    private String text;
    private Long createdAt;
    private Long updatedAt;
    private List<ScoreDto> scores;

    public UserReviewSummaryDto() {}

    public static UserReviewSummaryDto fromEntity(ReviewEntity entity) {
        if (entity == null) return null;

        List<ScoreDto> scoreList = entity.getScores() != null ?
                entity.getScores().stream()
                        .map(s -> new ScoreDto(s.getType(), s.getScore()))
                        .toList() : Collections.emptyList();

        UserReviewSummaryDto dto = new UserReviewSummaryDto();
        dto.setReviewId(entity.getId());
        dto.setUserName(entity.getAuthorName());
        dto.setRating(entity.getRating());
        dto.setText(entity.getText());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setScores(scoreList);
        dto.setUpdatedAt(entity.getUpdateAt());
        return dto;
    }


    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<ScoreDto> getScores() { return scores; }
    public void setScores(List<ScoreDto> scores) { this.scores = scores; }


    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}