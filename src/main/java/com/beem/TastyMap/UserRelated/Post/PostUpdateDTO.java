package com.beem.TastyMap.UserRelated.Post;

public class PostUpdateDTO {
    private String explanation;
    private Integer puan;
    private String photoUrl;

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
}
