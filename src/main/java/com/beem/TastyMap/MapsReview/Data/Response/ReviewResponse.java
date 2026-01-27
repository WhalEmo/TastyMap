package com.beem.TastyMap.MapsReview.Data.Response;

import com.beem.TastyMap.MapsReview.Data.ReviewResult;

import java.util.List;

public class ReviewResponse {
    private int page;
    private int size;
    private List<ReviewResult> reviewList;
    private String placeId;

    public ReviewResponse(int page, int size, List<ReviewResult> reviewList, String placeId) {
        this.page = page;
        this.size = size;
        this.reviewList = reviewList;
        this.placeId = placeId;
    }

    public ReviewResponse() {
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<ReviewResult> getReviewList() {
        return reviewList;
    }

    public void setReviewList(List<ReviewResult> reviewList) {
        this.reviewList = reviewList;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
