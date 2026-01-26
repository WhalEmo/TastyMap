package com.beem.TastyMap.Maps.Data;


import com.beem.TastyMap.Maps.Entity.PlaceEntity;

import java.util.List;

public class PlaceDetailsResult {
    private String place_id;
    private String name;
    private Double rating;
    private Integer user_ratings_total;
    private Integer price_level;
    private List<String> types;

    private String formatted_phone_number;
    private String international_phone_number;
    private String website;

    private OpeningHours opening_hours;
    private Geometry geometry;
    private List<Photo> photos;
    private List<Review> reviews;

    private String formatted_address;

    public static PlaceDetailsResult fromEntity(PlaceEntity entity) {
        PlaceDetailsResult result = new PlaceDetailsResult();

        result.setPlace_id(entity.getPlaceId());
        result.setName(entity.getName());
        result.setRating(entity.getRating());
        result.setUser_ratings_total(entity.getUserRatingsTotal());
        result.setPrice_level(entity.getPriceLevel());
        result.setFormatted_address(entity.getFormattedAddress());

        if (entity.getTypes() != null) {
            result.setTypes(entity.getTypes().stream().toList());
        }

        Location location = new Location();
        location.setLat(entity.getLatitude());
        location.setLng(entity.getLongitude());

        Geometry geo = new Geometry();
        geo.setLocation(location);
        result.setGeometry(geo);

        if (entity.getPhotos() != null) {
            result.setPhotos(
                    entity.getPhotos()
                            .stream()
                            .map(Photo::fromEntity)
                            .toList()
            );
        }

        result.setFormatted_phone_number(entity.getFormattedPhoneNumber());
        result.setInternational_phone_number(entity.getInternationalPhoneNumber());
        result.setWebsite(entity.getWebsite());

        if(entity.getOpeningHours() != null){
            result.opening_hours = new OpeningHours(
                    entity.getOpeningHours().getOpenNow(),
                    entity.getOpeningHours().getWeekdayText()
            );
        }

        if(entity.getReviews() != null){
            result.setReviews(
                    entity.getReviews()
                            .stream()
                            .map(Review::fromEntity)
                            .toList()
            );
        }

        return result;
    }

    public PlaceDetailsResult() {
    }

    public String getFormatted_address() {
        return formatted_address;
    }

    public void setFormatted_address(String formatted_address) {
        this.formatted_address = formatted_address;
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

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getUser_ratings_total() {
        return user_ratings_total;
    }

    public void setUser_ratings_total(Integer user_ratings_total) {
        this.user_ratings_total = user_ratings_total;
    }

    public Integer getPrice_level() {
        return price_level;
    }

    public void setPrice_level(Integer price_level) {
        this.price_level = price_level;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public String getFormatted_phone_number() {
        return formatted_phone_number;
    }

    public void setFormatted_phone_number(String formatted_phone_number) {
        this.formatted_phone_number = formatted_phone_number;
    }

    public String getInternational_phone_number() {
        return international_phone_number;
    }

    public void setInternational_phone_number(String international_phone_number) {
        this.international_phone_number = international_phone_number;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public OpeningHours getOpening_hours() {
        return opening_hours;
    }

    public void setOpening_hours(OpeningHours opening_hours) {
        this.opening_hours = opening_hours;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
}
