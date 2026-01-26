package com.beem.TastyMap.UserProfile.Post.Comments;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false,length = 500)
    private String contents;

    private LocalDateTime date;

    private Long parentYorumId;

    @PrePersist
    public void prePersist() {
        this.date = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getParentYorumId() {
        return parentYorumId;
    }

    public void setParentYorumId(Long parentYorumId) {
        this.parentYorumId = parentYorumId;
    }
}
