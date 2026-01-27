package com.beem.TastyMap.UserProfile.Post.Like;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.UserProfile.Post.PostEntity;
import jakarta.persistence.*;

@Entity
@Table(name="postLikes",uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "user_id"})})
public class LikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_likes_id")
    private Long postLikesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public Long getPostLikesId() {
        return postLikesId;
    }

    public void setPostLikesId(Long postLikesId) {
        this.postLikesId = postLikesId;
    }

    public PostEntity getPost() {
        return post;
    }

    public void setPost(PostEntity post) {
        this.post = post;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}
