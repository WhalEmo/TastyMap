package com.beem.TastyMap.post.dto;

import java.time.LocalDateTime;

public class PostResponseDTO {
    private boolean commentEnabled;
    private Long postId;
    private String explanation;
    private int point;
    private String photoUrl;
    private int numberof_likes;
    private LocalDateTime createdAt;
    private LocalDateTime updateDate;

    private Long userId;
    private String username;
    private String profilePhotoUrl;

    private String placeId;
    private String placeName;
    private String categories;
    private String city;
    private String district;
    private String neighbourhood;
    private double latitude;
    private double longitude;
    private double averagePoint;
    private boolean isLiked;
    private int commentCount;
    private boolean isPinned;

    public PostResponseDTO() {
    }

    public PostResponseDTO(boolean commentEnabled, Long postId, String explanation, int point, String photoUrl, int numberof_likes, LocalDateTime createdAt, LocalDateTime updateDate, Long userId, String username, String profilePhotoUrl, String placeId, String placeName, String categories, String city, String district, String neighbourhood, double latitude, double longitude, double averagePoint, boolean isLiked, int commentCount, boolean isPinned) {
        this.commentEnabled=commentEnabled;
        this.postId = postId;
        this.explanation = explanation;
        this.point = point;
        this.photoUrl = photoUrl;
        this.numberof_likes = numberof_likes;
        this.createdAt = createdAt;
        this.updateDate = updateDate;
        this.userId = userId;
        this.username = username;
        this.profilePhotoUrl = profilePhotoUrl;
        this.placeId = placeId;
        this.placeName = placeName;
        this.categories = categories;
        this.city = city;
        this.district = district;
        this.neighbourhood = neighbourhood;
        this.latitude = latitude;
        this.longitude = longitude;
        this.averagePoint = averagePoint;
        this.isLiked=isLiked;
        this.commentCount=commentCount;
        this.isPinned = isPinned;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getNumberof_likes() {
        return numberof_likes;
    }

    public void setNumberof_likes(int numberof_likes) {
        this.numberof_likes = numberof_likes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
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

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getNeighbourhood() {
        return neighbourhood;
    }

    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAveragePoint() {
        return averagePoint;
    }

    public void setAveragePoint(double averagePoint) {
        this.averagePoint = averagePoint;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }
}
