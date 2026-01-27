package com.beem.TastyMap.MapsReview.Data;

import com.beem.TastyMap.MapsReview.Enum.ReviewSource;
import com.beem.TastyMap.MapsReview.Entity.ReviewEntity;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResult {
    private Long id;
    private String name;
    private String userProfile;
    private double rating;
    private String content;
    private ReviewSource source;
    private Long parentId;
    private Integer likeCount;
    private Long createdAt;
    private Long updateAt;


    public static ReviewResult fromEntity(ReviewEntity entity){
        ReviewResult review = new ReviewResult();

        review.id = entity.getId();

        if(entity.getSource()==ReviewSource.GOOGLE){
            review.name = entity.getAuthorName();
            review.userProfile = null;
        }
        else{
            review.name = entity.getUser().getUsername();
            review.userProfile = entity.getUser().getProfilePhotoUrl();
            review.parentId = entity.getParent() != null ? entity.getParent().getId()
                    : null;
        }

        review.rating = entity.getRating();
        review.content = entity.getText();
        review.source = entity.getSource();
        review.likeCount = entity.getLikeCount();
        review.createdAt = entity.getCreatedAt();
        review.updateAt = entity.getUpdateAt();

        return review;
    }

    public ReviewResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ReviewSource getSource() {
        return source;
    }

    public void setSource(ReviewSource source) {
        this.source = source;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Long updateAt) {
        this.updateAt = updateAt;
    }
}
