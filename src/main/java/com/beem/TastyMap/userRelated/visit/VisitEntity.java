package com.beem.TastyMap.userRelated.visit;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.post.PlaceEmbedded;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_visit")
public class VisitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Embedded
    private PlaceEmbedded placeEmbedded;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean isDelete=false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public PlaceEmbedded getPlaceEmbedded() {
        return placeEmbedded;
    }

    public void setPlaceEmbedded(PlaceEmbedded placeEmbedded) {
        this.placeEmbedded = placeEmbedded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean delete) {
        isDelete = delete;
    }
}
