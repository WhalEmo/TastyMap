package com.beem.TastyMap.UserRelated.Post;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.UserRelated.Post.Comments.CommentEntity;
import com.beem.TastyMap.UserRelated.Post.Like.PostLikeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_post_user_created", columnList = "user_id, createdAt")
        }
)
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(length = 500)
    private String explanation;

    private Integer puan;

    private String photoUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "number_of_likes")
    private int numberofLikes = 0;

    @Column(nullable = false)
    private boolean commentEnabled = true;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Embedded
    private PlaceEmbedded placeEmbedded;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentEntity> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLikeEntity> likes=new ArrayList<>();


    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

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

    public List<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(List<CommentEntity> comments) {
        this.comments = comments;
    }

    public int getNumberofLikes() {
        return numberofLikes;
    }

    public void setNumberofLikes(int numberofLikes) {
        this.numberofLikes = numberofLikes;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }

    public List<PostLikeEntity> getLikes() {
        return likes;
    }

    public void setLikes(List<PostLikeEntity> likes) {
        this.likes = likes;
    }
}
