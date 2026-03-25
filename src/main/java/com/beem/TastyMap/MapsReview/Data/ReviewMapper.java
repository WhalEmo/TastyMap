package com.beem.TastyMap.MapsReview.Data;

import com.beem.TastyMap.MapsReview.Data.Response.UpdatedReviewRes;
import com.beem.TastyMap.MapsReview.Entity.ReviewEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ReviewMapper {

    public UpdatedReviewRes toUpdatedReviewRes(ReviewEntity review) {
        return new UpdatedReviewRes(
                review.getId(),
                review.getPlace().getId(),
                review.getAuthorName(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdateAt(),
                review.getScores().stream()
                        .map(s -> new ScoreDto(
                                s.getType(),
                                s.getScore()
                        ))
                        .toList()
        );
    }

    public Map<String, Object> toDeletedReviewRes(){
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Deleted successfully.");
        response.put("status", "Ok");
        response.put("isDeleted",true);
        return response;
    }
}
