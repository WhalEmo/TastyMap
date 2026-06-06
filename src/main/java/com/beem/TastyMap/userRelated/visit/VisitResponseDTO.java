package com.beem.TastyMap.userRelated.visit;

import java.time.LocalDateTime;

public class VisitResponseDTO {
    private Long visitId;
    private LocalDateTime createdAt;

    private String placeId;
    private String placeName;
    private String categories;
    private String city;
    private String district;
    private String neighbourhood;
    private double latitude;
    private double longitude;
    private double averagePuan;

    public VisitResponseDTO(Long visitId, LocalDateTime createdAt, String placeId, String placeName, String categories, String city, String district, String neighbourhood, double latitude, double longitude, double averagePuan) {
        this.visitId = visitId;
        this.createdAt = createdAt;
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

    public Long getVisitId() {
        return visitId;
    }

    public void setVisitId(Long visitId) {
        this.visitId = visitId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public double getAveragePuan() {
        return averagePuan;
    }

    public void setAveragePuan(double averagePuan) {
        this.averagePuan = averagePuan;
    }


}
