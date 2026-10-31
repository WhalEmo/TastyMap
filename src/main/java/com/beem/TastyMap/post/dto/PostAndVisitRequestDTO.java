package com.beem.TastyMap.post.dto;

import com.beem.TastyMap.visit.dto.VisitRequestDTO;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PostAndVisitRequestDTO extends VisitRequestDTO {
    @Size(max = 500, message = "{validation.post.explanation.size}")
    private String explanation;
    private List<String> photoUrl;
    private boolean commentEnabled;


    public PostAndVisitRequestDTO() {
    }

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

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }
}
