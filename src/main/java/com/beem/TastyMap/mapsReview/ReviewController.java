package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.BaseApiResponse;
import com.beem.TastyMap.ai.AiSummaryResponse;
import com.beem.TastyMap.ai.AiSummaryService;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.data.request.SentReviewReq;
import com.beem.TastyMap.mapsReview.data.request.UpdateReviewReq;
import com.beem.TastyMap.mapsReview.data.response.CreatedReviewRes;
import com.beem.TastyMap.mapsReview.data.response.ReviewResponse;
import com.beem.TastyMap.mapsReview.data.response.UpdatedReviewRes;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/place-review")
public class ReviewController {

    private final ReviewService service;
    private final AiSummaryService aiSummaryService;

    public ReviewController(ReviewService service, AiSummaryService aiSummaryService) {
        this.service = service;
        this.aiSummaryService = aiSummaryService;
    }

    @GetMapping("/place-reviews/{placeId}")
    public BaseApiResponse<ReviewResponse> getPlaceReviews(
            @PathVariable String placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return BaseApiResponse.success(
                service.getPlaceReviews(
                        placeId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/place/{placeId}/ai-summary")
    public ResponseEntity<AiSummaryResponse> getSummary(
            @PathVariable Long placeId
    ) {
        List<ReviewEntity> optimalAiReviews = service.getProportionalReviewsForAi(placeId);

        if (optimalAiReviews == null || optimalAiReviews.isEmpty()) {
            return ResponseEntity.ok(new AiSummaryResponse(false, null));
        }

        AiSummaryResponse summaryResponse = aiSummaryService.getRestaurantSummary(optimalAiReviews);

        return ResponseEntity.ok(summaryResponse);
    }

    @PostMapping("/send-review")
    public BaseApiResponse<ReviewResult> sendPlaceReview(
            @RequestBody SentReviewReq request,
            Authentication authentication
    ){
        return BaseApiResponse
                .success(
                        service.sendPlaceReview(
                                request,
                                (Long) authentication.getPrincipal()
                        )
                );
    }

    @PatchMapping("/update-review")
    public BaseApiResponse<ReviewResult> updatePlaceReview(
            @RequestBody UpdateReviewReq request,
            Authentication authentication
    ){
        return BaseApiResponse
                .success(
                        service.patchPlaceReview(
                                request,
                                (Long) authentication.getPrincipal()
                        )
                );
    }
    @DeleteMapping("/delete-review/{reviewID}")
    public BaseApiResponse<Boolean> deletePlaceReview(
            @PathVariable Long reviewID,
            Authentication authentication
    ){
        service.deletePlaceReview(
                reviewID,
                (Long) authentication.getPrincipal()
        );
        return BaseApiResponse
                .success(
                        true,
                        "Review deleted successfully."
                );
    }
}
