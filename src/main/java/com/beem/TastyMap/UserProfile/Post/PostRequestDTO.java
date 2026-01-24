package com.beem.TastyMap.UserProfile.Post;

import jakarta.persistence.Column;

public class PostRequestDTO {
    private String explanation;
    private Integer puan;
    private String photoUrl;

    private String placeId;
    private String placeName;
    private String categories;
    private String city;
    private String district;
    private String neighbourhood;
    private Double latitude;
    private Double longitude;
    private Double averagePuan;

    public PostRequestDTO() {
    }

    public PostRequestDTO(String explanation, Integer puan, String placeId, String placeName, String categories, String city, String district, String neighbourhood, Double latitude, Double longitude, Double averagePuan,String photoUrl) {
        this.explanation = explanation;
        this.puan = puan;
        this.placeId = placeId;
        this.placeName = placeName;
        this.categories = categories;
        this.city = city;
        this.district = district;
        this.neighbourhood = neighbourhood;
        this.latitude = latitude;
        this.longitude = longitude;
        this.averagePuan = averagePuan;
        this.photoUrl=photoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Double getAveragePuan() {
        return averagePuan;
    }

    public void setAveragePuan(Double averagePuan) {
        this.averagePuan = averagePuan;
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

}
