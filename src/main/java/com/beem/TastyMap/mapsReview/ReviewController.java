package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.mapsReview.data.Request.SentReviewReq;
import com.beem.TastyMap.mapsReview.data.Response.CreatedReviewRes;
import com.beem.TastyMap.mapsReview.data.Response.ReviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CreatedReviewRes> sendPlaceReview(@RequestBody SentReviewReq request){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.sendPlaceReview(request));
    }

    @PatchMapping("/update-review")
    public ResponseEntity<?> updatePlaceReview(@RequestBody SentReviewReq request){
        return null;
    }
}
