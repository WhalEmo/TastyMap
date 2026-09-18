package com.beem.TastyMap.maps.data.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.beem.TastyMap.maps.data.Geometry;
import com.beem.TastyMap.maps.data.OpeningHours;
import com.beem.TastyMap.maps.data.Review;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaceDetailsDto {
    private String place_id;
    private String name;
    private Double rating;

    @JsonProperty("user_ratings_total")
    private Integer userRatingsTotal;

    @JsonProperty("price_level")
    private Integer priceLevel;

    private List<String> types;

    @JsonProperty("formatted_phone_number")
    private String formattedPhoneNumber;

    @JsonProperty("international_phone_number")
    private String internationalPhoneNumber;

    private String website;

    @JsonProperty("opening_hours")
    private OpeningHours openingHours;

    private Geometry geometry;
    private List<Review> reviews;

    @JsonProperty("formatted_address")
    private String formattedAddress;

    // Getter & Setter
    public String getPlace_id() { return place_id; }
    public void setPlace_id(String place_id) { this.place_id = place_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getUserRatingsTotal() { return userRatingsTotal; }
    public void setUserRatingsTotal(Integer userRatingsTotal) { this.userRatingsTotal = userRatingsTotal; }
    public Integer getPriceLevel() { return priceLevel; }
    public void setPriceLevel(Integer priceLevel) { this.priceLevel = priceLevel; }
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
    public String getFormattedPhoneNumber() { return formattedPhoneNumber; }
    public void setFormattedPhoneNumber(String formattedPhoneNumber) { this.formattedPhoneNumber = formattedPhoneNumber; }
    public String getInternationalPhoneNumber() { return internationalPhoneNumber; }
    public void setInternationalPhoneNumber(String internationalPhoneNumber) { this.internationalPhoneNumber = internationalPhoneNumber; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public OpeningHours getOpeningHours() { return openingHours; }
    public void setOpeningHours(OpeningHours openingHours) { this.openingHours = openingHours; }
    public Geometry getGeometry() { return geometry; }
    public void setGeometry(Geometry geometry) { this.geometry = geometry; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public String getFormattedAddress() { return formattedAddress; }
    public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }
}