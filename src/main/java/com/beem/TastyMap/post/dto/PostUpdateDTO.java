package com.beem.TastyMap.post.dto;

import jakarta.validation.constraints.Size;

public class PostUpdateDTO {
    @Size(max = 500, message = "{validation.post.explanation.size}")
    private String explanation;

    private Integer point;
    private String photoUrl;

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}