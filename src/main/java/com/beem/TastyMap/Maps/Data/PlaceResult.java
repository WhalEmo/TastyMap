package com.beem.TastyMap.Maps.Data;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceResult {
    private String name;
    private String vicinity;
    private Double rating;
    private Integer price_level;
    private Integer user_ratings_total;
    private String business_status;
    private String place_id;
    private List<String> types;

    private Geometry geometry;
    private OpeningHours opening_hours;
    private List<Photo> photos;


    public PlaceResult() {
    }

    public static PlaceResult fromEntity(PlaceEntity entity){
        PlaceResult result = new PlaceResult();
        result.setName(entity.getName());
        result.setVicinity(entity.getVicinity());
        result.setRating(entity.getRating());
        result.setPrice_level(entity.getPriceLevel());
        result.setUser_ratings_total(entity.getUserRatingsTotal());
        result.setBusiness_status(entity.getBusinessStatus());
        result.setPlace_id(entity.getPlaceId());
        result.setTypes(entity.getTypes().stream().toList());

        Location location = new Location();
        location.setLat(entity.getLatitude());
        location.setLng(entity.getLongitude());

        Geometry geo = new Geometry();
        geo.setLocation(location);

        result.setGeometry(geo);

        result.setPhotos(entity.getPhotos().stream().map(Photo::fromEntity).toList());

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVicinity() {
        return vicinity;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getPrice_level() {
        return price_level;
    }

    public void setPrice_level(Integer price_level) {
        this.price_level = price_level;
    }

    public Integer getUser_ratings_total() {
        return user_ratings_total;
    }

    public void setUser_ratings_total(Integer user_ratings_total) {
        this.user_ratings_total = user_ratings_total;
    }

    public String getBusiness_status() {
        return business_status;
    }

    public void setBusiness_status(String business_status) {
        this.business_status = business_status;
    }

    public String getPlace_id() {
        return place_id;
    }

    public void setPlace_id(String place_id) {
        this.place_id = place_id;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public OpeningHours getOpening_hours() {
        return opening_hours;
    }

    public void setOpening_hours(OpeningHours opening_hours) {
        this.opening_hours = opening_hours;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
}
