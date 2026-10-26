package com.beem.TastyMap.rag.data.document;

import java.util.*;

public class RestaurantMetadata {

    private String name = "Bilinmeyen Mekan";
    private String placeId;
    private Long dbId;
    private Double latitude;
    private Double longitude;
    private Double googleRating = 0.0;
    private Double tastymapRating = 0.0;
    private Double distanceKm;
    private Double vectorScore = 0.0;
    private Double compositeScore = 0.0;
    private List<String> types = new ArrayList<>();

    private Boolean isDining = true;
    private VenueCategory venueCategory = VenueCategory.RESTAURANT;
    private Boolean veganFriendly = false;
    private Boolean vegetarianFriendly = false;
    private Boolean glutenFreeFriendly = false;
    private List<String> dietaryTags = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public Long getDbId() { return dbId; }
    public void setDbId(Long dbId) { this.dbId = dbId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getGoogleRating() { return googleRating; }
    public void setGoogleRating(Double googleRating) { this.googleRating = googleRating; }

    public Double getTastymapRating() { return tastymapRating; }
    public void setTastymapRating(Double tastymapRating) { this.tastymapRating = tastymapRating; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getVectorScore() { return vectorScore; }
    public void setVectorScore(Double vectorScore) { this.vectorScore = vectorScore; }

    public Double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(Double compositeScore) { this.compositeScore = compositeScore; }

    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }

    public Boolean getDining() { return isDining; }
    public void setDining(Boolean dining) { isDining = dining; }

    public VenueCategory getVenueCategory() { return venueCategory; }
    public void setVenueCategory(VenueCategory venueCategory) {
        this.venueCategory = venueCategory != null ? venueCategory : VenueCategory.RESTAURANT;
    }

    // String üzerinden doğrudan set edebilmek için yardımcı setter
    public void setVenueCategoryFromString(String categoryStr) {
        this.venueCategory = VenueCategory.fromKey(categoryStr);
    }

    public Boolean getVeganFriendly() { return veganFriendly; }
    public void setVeganFriendly(Boolean veganFriendly) { this.veganFriendly = veganFriendly; }

    public Boolean getVegetarianFriendly() { return vegetarianFriendly; }
    public void setVegetarianFriendly(Boolean vegetarianFriendly) { this.vegetarianFriendly = vegetarianFriendly; }

    public Boolean getGlutenFreeFriendly() { return glutenFreeFriendly; }
    public void setGlutenFreeFriendly(Boolean glutenFreeFriendly) { this.glutenFreeFriendly = glutenFreeFriendly; }

    public List<String> getDietaryTags() { return dietaryTags; }
    public void setDietaryTags(List<String> dietaryTags) { this.dietaryTags = dietaryTags != null ? dietaryTags : new ArrayList<>(); }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("place_id", placeId);
        map.put("db_id", dbId);
        if (latitude != null && longitude != null) {
            map.put("location", Map.of("lat", latitude, "lon", longitude));
            map.put("latitude", latitude);
            map.put("longitude", longitude);
        }
        map.put("google_rating", googleRating);
        map.put("tastymap_rating", tastymapRating);
        map.put("calculated_distance_km", distanceKm);
        map.put("score", vectorScore);
        map.put("tastymap_composite_score", compositeScore);
        map.put("types", String.join(", ", types));

        map.put("is_dining", isDining);
        map.put("venue_category", venueCategory.getKey());
        map.put("is_vegan_friendly", veganFriendly);
        map.put("is_vegetarian_friendly", vegetarianFriendly);
        map.put("is_gluten_free_friendly", glutenFreeFriendly);
        map.put("dietary_tags", dietaryTags);

        return map;
    }

    @Override
    public String toString() {
        return String.format("'%s' [%s] | Puan: %.1f | Mesafe: %.2f km | Vektör: %.4f | Final: %.4f | Vegan: %s | Vegetarian: %s | Gluten: %s",
                name, venueCategory.getKey(), googleRating, distanceKm != null ? distanceKm : 0.0,
                vectorScore, compositeScore, veganFriendly, vegetarianFriendly, glutenFreeFriendly);
    }
}