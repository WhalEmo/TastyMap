package com.beem.TastyMap.userRelated.visit;

public class VisitRequestDTO {
    private String placeId;
    private String placeName;
    private String categories;
    private String city;
    private String district;
    private String neighbourhood;
    private Double latitude;
    private Double longitude;
    private Double averagePuan;
    private boolean isWantToPost;

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

    public boolean isWantToPost() {
        return isWantToPost;
    }

    public void setWantToPost(boolean wantToPost) {
        isWantToPost = wantToPost;
    }
}
