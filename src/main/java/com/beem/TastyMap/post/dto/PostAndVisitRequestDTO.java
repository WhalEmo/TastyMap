package com.beem.TastyMap.post.dto;

import com.beem.TastyMap.visit.dto.VisitRequestDTO;
import jakarta.validation.constraints.Size;

public class PostAndVisitRequestDTO extends VisitRequestDTO {
    @Size(max = 500, message = "{validation.post.explanation.size}")
    private String explanation;
    private String photoUrl;
    private boolean commentEnabled;


    public PostAndVisitRequestDTO() {
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }


    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }
}
