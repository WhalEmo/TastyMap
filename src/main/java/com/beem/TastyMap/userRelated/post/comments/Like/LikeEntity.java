package com.beem.TastyMap.userRelated.post.comments.Like;


import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.post.comments.CommentEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "comment_like", uniqueConstraints = {@UniqueConstraint(columnNames = {"comment_id", "user_id"})})
public class LikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_likes_id")
    private Long commentLikesId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public Long getCommentLikesId() {
        return commentLikesId;
    }

    public void setCommentLikesId(Long commentLikesId) {
        this.commentLikesId = commentLikesId;
    }

    public CommentEntity getComment() {
        return comment;
    }

    public void setComment(CommentEntity comment) {
        this.comment = comment;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}
