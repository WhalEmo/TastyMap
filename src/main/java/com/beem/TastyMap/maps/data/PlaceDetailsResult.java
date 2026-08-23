package com.beem.TastyMap.maps.data;


import com.beem.TastyMap.maps.data.google.GooglePlaceDetailsDto;
import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.mapsReview.data.response.UserReviewSummaryDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceDetailsResult {
    private String place_id;
    private String name;

    private Double googleRating;

    private Integer googleReviewCount;

    private Double tastyMapRating;

    private Integer tastyMapReviewCount;

    private Integer priceLevel;
    private List<String> types;

    private String formattedPhoneNumber;
    private String internationalPhoneNumber;
    private String website;

    private OpeningHours openingHours;
    private Geometry geometry;
    private List<Review> reviews;

    private UserReviewSummaryDto userReview;

    private String formattedAddress;

    public static PlaceDetailsResult fromEntity(PlaceEntity entity) {
        PlaceDetailsResult result = new PlaceDetailsResult();

        result.setPlace_id(entity.getPlaceId());
        result.setName(entity.getName());

        result.setGoogleRating(entity.getGoogleRating());
        result.setGoogleReviewCount(entity.getGoogleReviewCount());

        result.setTastyMapRating(entity.getTastyMapRating());
        result.setTastyMapReviewCount(entity.getTastyMapReviewCount());

        result.setPriceLevel(entity.getPriceLevel());
        result.setFormattedAddress(entity.getFormattedAddress());

        if (entity.getTypes() != null) {
            result.setTypes(entity.getTypes().stream().toList());
        }

        Location location = new Location();
        location.setLat(entity.getLatitude());
        location.setLng(entity.getLongitude());

        Geometry geo = new Geometry();
        geo.setLocation(location);
        result.setGeometry(geo);


        result.setFormattedPhoneNumber(entity.getFormattedPhoneNumber());
        result.setInternationalPhoneNumber(entity.getInternationalPhoneNumber());
        result.setWebsite(entity.getWebsite());

        if(entity.getOpeningHours() != null){
            result.openingHours = new OpeningHours(
                    entity.getOpeningHours().getOpenNow(),
                    entity.getOpeningHours().getWeekdayText()
            );
        }


        return result;
    }

    public static PlaceDetailsResult fromGoogleDto(GooglePlaceDetailsDto dto) {
        PlaceDetailsResult result = new PlaceDetailsResult();
        result.setPlace_id(dto.getPlace_id());
        result.setName(dto.getName());
        result.setGoogleRating(dto.getRating() != null ? dto.getRating() : 0.0);
        result.setGoogleReviewCount(dto.getUserRatingsTotal() != null ? dto.getUserRatingsTotal() : 0);
        result.setPriceLevel(dto.getPriceLevel());
        result.setTypes(dto.getTypes());
        result.setFormattedPhoneNumber(dto.getFormattedPhoneNumber());
        result.setInternationalPhoneNumber(dto.getInternationalPhoneNumber());
        result.setWebsite(dto.getWebsite());
        result.setOpeningHours(dto.getOpeningHours());
        result.setGeometry(dto.getGeometry());
        result.setReviews(dto.getReviews());
        result.setFormattedAddress(dto.getFormattedAddress());
        return result;
    }

    public PlaceDetailsResult() {
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getPlace_id() {
        return place_id;
    }

    public void setPlace_id(String place_id) {
        this.place_id = place_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public String getFormattedPhoneNumber() {
        return formattedPhoneNumber;
    }

    public void setFormattedPhoneNumber(String formattedPhoneNumber) {
        this.formattedPhoneNumber = formattedPhoneNumber;
    }

    public String getInternationalPhoneNumber() {
        return internationalPhoneNumber;
    }

    public void setInternationalPhoneNumber(String internationalPhoneNumber) {
        this.internationalPhoneNumber = internationalPhoneNumber;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public OpeningHours getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(OpeningHours openingHours) {
        this.openingHours = openingHours;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }


    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public UserReviewSummaryDto getUserReview() {
        return userReview;
    }

    public void setUserReview(UserReviewSummaryDto userReview) {
        this.userReview = userReview;
    }

    public Double getGoogleRating() {
        return googleRating;
    }

    public void setGoogleRating(Double googleRating) {
        this.googleRating = googleRating;
    }

    public Integer getGoogleReviewCount() {
        return googleReviewCount;
    }

    public void setGoogleReviewCount(Integer googleReviewCount) {
        this.googleReviewCount = googleReviewCount;
    }

    public Double getTastyMapRating() {
        return tastyMapRating;
    }

    public void setTastyMapRating(Double tastyMapRating) {
        this.tastyMapRating = tastyMapRating;
    }

    public Integer getTastyMapReviewCount() {
        return tastyMapReviewCount;
    }

    public void setTastyMapReviewCount(Integer tastyMapReviewCount) {
        this.tastyMapReviewCount = tastyMapReviewCount;
    }
}
