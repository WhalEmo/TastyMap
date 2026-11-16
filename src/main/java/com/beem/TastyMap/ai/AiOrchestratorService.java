package com.beem.TastyMap.ai;

import com.beem.TastyMap.mapsReview.ReviewService;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiOrchestratorService {
    private final ReviewService reviewService;
    private final AiSummaryService aiSummaryService;

    public AiOrchestratorService(ReviewService reviewService, AiSummaryService aiSummaryService) {
        this.reviewService = reviewService;
        this.aiSummaryService = aiSummaryService;
    }
    @Cacheable(value = "ai_summaries", key = "#placeId")
    public AiSummaryResponse getCachedAiSummary(Long placeId) {

        List<ReviewEntity> reviews = reviewService.getProportionalReviewsForAi(placeId);

        if (reviews == null || reviews.isEmpty()) {
            return new AiSummaryResponse(false, null);
        }

        return aiSummaryService.getRestaurantSummary(reviews);
    }
}
