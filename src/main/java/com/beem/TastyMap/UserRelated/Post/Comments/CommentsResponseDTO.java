package com.beem.TastyMap.UserRelated.Post.Comments;

import java.time.LocalDateTime;

public class CommentsResponseDTO {
    private Long id;
    private Long parentCommentId;
    private Long post_id;
    private String contents;
    private LocalDateTime date;
    private int numberOfLikes;

    private Long userId;
    private String username;
    private String profilePhotoUrl;


    public CommentsResponseDTO(Long id, Long parentCommentId, Long post_id, String contents, LocalDateTime date, int numberOfLikes, Long userId, String username, String profilePhotoUrl) {
        this.id = id;
        this.parentCommentId = parentCommentId;
        this.post_id = post_id;
        this.contents = contents;
        this.date = date;
        this.numberOfLikes = numberOfLikes;
        this.userId = userId;
        this.username = username;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public CommentsResponseDTO(CommentEntity comment) {
        this.id = comment.getId();
        this.post_id = comment.getPost().getId();
        this.userId=comment.getUser().getId();
        this.username=comment.getUser().getUsername();
        this.profilePhotoUrl=comment.getUser().getProfile();
        this.contents = comment.getContents();
        this.numberOfLikes = comment.getNumberofLikes();
        this.date = comment.getDate();
        this.parentCommentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;
    }
    public CommentsResponseDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public Long getPost_id() {
        return post_id;
    }

    public void setPost_id(Long post_id) {
        this.post_id = post_id;
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

    public int getNumberOfLikes() {
        return numberOfLikes;
    }

    public void setNumberOfLikes(int numberOfLikes) {
        this.numberOfLikes = numberOfLikes;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
