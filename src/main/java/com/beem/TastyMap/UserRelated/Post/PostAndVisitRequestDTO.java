package com.beem.TastyMap.UserRelated.Post;

import com.beem.TastyMap.UserRelated.Visit.VisitRequestDTO;
import jakarta.validation.constraints.Size;

public class PostAndVisitRequestDTO extends VisitRequestDTO {
    @Size(max = 500, message = "Açıklama max 500 karakter olmalıdır")
    private String explanation;
    private Integer puan;
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

    public Integer getPuan() {
        return puan;
    }

    public void setPuan(Integer puan) {
        this.puan = puan;
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
