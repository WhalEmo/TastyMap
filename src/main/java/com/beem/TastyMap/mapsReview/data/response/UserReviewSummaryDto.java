package com.beem.TastyMap.mapsReview.data.response;

import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;

import java.util.Collections;
import java.util.List;

public class UserReviewSummaryDto {
    private Long review_id;
    private String author_name;
    private Double rating;
    private String text;
    private Long created_at;
    private List<ScoreDto> scores;

    public UserReviewSummaryDto() {}

    public static UserReviewSummaryDto fromEntity(ReviewEntity entity) {
        if (entity == null) return null;

        List<ScoreDto> scoreList = entity.getScores() != null ?
                entity.getScores().stream()
                        .map(s -> new ScoreDto(s.getType(), s.getScore()))
                        .toList() : Collections.emptyList();

        UserReviewSummaryDto dto = new UserReviewSummaryDto();
        dto.setReview_id(entity.getId());
        dto.setAuthor_name(entity.getAuthorName());
        dto.setRating(entity.getRating());
        dto.setText(entity.getText());
        dto.setCreated_at(entity.getCreatedAt());
        dto.setScores(scoreList);
        return dto;
    }

    public Long getReview_id() { return review_id; }
    public void setReview_id(Long review_id) { this.review_id = review_id; }

    public String getAuthor_name() { return author_name; }
    public void setAuthor_name(String author_name) { this.author_name = author_name; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Long getCreated_at() { return created_at; }
    public void setCreated_at(Long created_at) { this.created_at = created_at; }

    public List<ScoreDto> getScores() { return scores; }
    public void setScores(List<ScoreDto> scores) { this.scores = scores; }
}