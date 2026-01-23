package com.beem.TastyMap.UserProfile.Post;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 500)
    private String explanation;

    private Integer puan;

    private String photoUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Embedded
    private PlaceEmbedded placeEmbedded;

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PlaceEmbedded getPlaceEmbedded() {
        return placeEmbedded;
    }

    public void setPlaceEmbedded(PlaceEmbedded placeEmbedded) {
        this.placeEmbedded = placeEmbedded;
    }
}
