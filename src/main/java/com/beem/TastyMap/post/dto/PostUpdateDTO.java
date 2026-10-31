package com.beem.TastyMap.post.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class PostUpdateDTO {
    @Size(max = 500, message = "{validation.post.explanation.size}")
    private String explanation;
    private boolean commentEnabled;

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }


    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }
}