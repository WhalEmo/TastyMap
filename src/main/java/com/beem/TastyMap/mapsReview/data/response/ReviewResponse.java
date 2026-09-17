package com.beem.TastyMap.mapsReview.data.response;

import com.beem.TastyMap.mapsReview.data.ReviewResult;

import java.util.List;

public record ReviewResponse(
        int page,
        int size,
        List<ReviewResult> reviewList,
        String placeId
) {}