package com.beem.TastyMap.MapsReview;

import com.beem.TastyMap.MapsReview.Data.Request.SentReviewReq;
import com.beem.TastyMap.MapsReview.Data.Request.UpdateReviewReq;
import com.beem.TastyMap.MapsReview.Data.Response.CreatedReviewRes;
import com.beem.TastyMap.MapsReview.Data.Response.UpdatedReviewRes;
import com.beem.TastyMap.MapsReview.Data.Response.ReviewResponse;
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
    public ResponseEntity<ReviewResponse> getPlaceReviews(
            @PathVariable String placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(
                service.getPlaceReviews(
                        placeId,
                        page,
                        size
                )
        );
    }

    @PostMapping("/send-review")
    public ResponseEntity<CreatedReviewRes> sendPlaceReview(
            @RequestBody SentReviewReq request,
            Authentication authentication
    ){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.sendPlaceReview(
                                request,
                                (Long) authentication.getPrincipal()
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
