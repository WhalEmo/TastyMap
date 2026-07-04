package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.BaseApiResponse;
import com.beem.TastyMap.mapsReview.data.request.SentReviewReq;
import com.beem.TastyMap.mapsReview.data.request.UpdateReviewReq;
import com.beem.TastyMap.mapsReview.data.response.CreatedReviewRes;
import com.beem.TastyMap.mapsReview.data.response.ReviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/place-review")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
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

    @PostMapping("/send-review")
    public BaseApiResponse<CreatedReviewRes> sendPlaceReview(
            @RequestBody SentReviewReq request,
            @RequestParam Long userId,
            Authentication authentication
    ){
        return BaseApiResponse
                .success(
                        service.sendPlaceReview(
                                request,
                                userId
                        )
                );
    }

    @PatchMapping("/update-review")
    public ResponseEntity<?> updatePlaceReview(
            @RequestBody UpdateReviewReq request,
            Authentication authentication
    ){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        service.patchPlaceReview(
                                request,
                                (Long) authentication.getPrincipal()
                        )
                );
    }
    @DeleteMapping("/delete-review/{reviewID}")
    public ResponseEntity<?> deletePlaceReview(
            @PathVariable Long reviewID,
            Authentication authentication
    ){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        service.deletePlaceReview(
                                reviewID,
                                (Long) authentication.getPrincipal()
                        )
                );
    }
}
