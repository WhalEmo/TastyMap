package com.beem.TastyMap.post.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class PostUpdateDTO {
    @Size(max = 500, message = "{validation.post.explanation.size}")
    private String explanation;
    private List<String> photoUrl;

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }


    public List<String> getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(List<String> photoUrl) {
        this.photoUrl = photoUrl;
    }
}