package com.beem.TastyMap.UserRelated.Post;

import java.time.LocalDateTime;

public class PostResponseDTO {
    private Long postId;
    private String explanation;
    private Integer puan;
    private String photoUrl;
    private int numberof_likes;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String profilePhotoUrl;

    private String placeId;
    private String placeName;
    private String categories;
    private String city;
    private String district;
    private String neighbourhood;
    private Double latitude;
    private Double longitude;
    private Double averagePuan;

    public PostResponseDTO() {
    }

    public PostResponseDTO(Long postId, String explanation, Integer puan, String photoUrl, int numberof_likes, LocalDateTime createdAt, Long userId, String username, String profilePhotoUrl, String placeId, String placeName, String categories, String city, String district, String neighbourhood, Double latitude, Double longitude, Double averagePuan) {
        this.postId = postId;
        this.explanation = explanation;
        this.puan = puan;
        this.photoUrl = photoUrl;
        this.numberof_likes = numberof_likes;
        this.createdAt = createdAt;
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
        this.averagePuan = averagePuan;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAveragePuan() {
        return averagePuan;
    }

    public void setAveragePuan(Double averagePuan) {
        this.averagePuan = averagePuan;
    }

    public int getNumberof_likes() {
        return numberof_likes;
    }

    public void setNumberof_likes(int numberof_likes) {
        this.numberof_likes = numberof_likes;
    }
}
