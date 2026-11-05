package com.beem.TastyMap.rag.data.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantMetadata {

    @JsonProperty("qdrant_id")
    private String qdrantId;

    @JsonProperty("name")
    private String name = "Bilinmeyen Mekan";

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("db_id")
    private Long dbId;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("google_rating")
    private Double googleRating = 0.0;

    @JsonProperty("tastymap_rating")
    private Double tastymapRating = 0.0;

    @JsonProperty("calculated_distance_km")
    private Double distanceKm;

    @JsonProperty("score")
    private Double vectorScore = 0.0;

    @JsonProperty("tastymap_composite_score")
    private Double compositeScore = 0.0;

    @JsonProperty("types")
    private List<String> types = new ArrayList<>();

    @JsonProperty("is_dining")
    private Boolean isDining = true;

    @JsonProperty("is_closed")
    private Boolean isClosed = false;

    @JsonProperty("venue_category")
    private VenueCategory venueCategory = VenueCategory.RESTAURANT;

    @JsonProperty("is_vegan_friendly")
    private Boolean veganFriendly = false;

    @JsonProperty("is_vegetarian_friendly")
    private Boolean vegetarianFriendly = false;

    @JsonProperty("is_gluten_free_friendly")
    private Boolean glutenFreeFriendly = false;

    @JsonProperty("dietary_tags")
    private List<String> dietaryTags = new ArrayList<>();

    @JsonProperty("highlighted_foods")
    private List<String> highlightedFoods = new ArrayList<>();

    @JsonProperty("atmosphere")
    private List<String> atmosphere = new ArrayList<>();

    @JsonProperty("service_notes")
    private List<String> serviceNotes = new ArrayList<>();

    @JsonProperty("cautions")
    private List<String> cautions = new ArrayList<>();

    public String getQdrantId() { return qdrantId; }
    public void setQdrantId(String qdrantId) { this.qdrantId = qdrantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public Long getDbId() { return dbId; }
    @JsonProperty("db_id")
    public void setDbId(Object rawDbId) {
        if (rawDbId == null) {
            this.dbId = null;
        } else if (rawDbId instanceof Number num) {
            this.dbId = num.longValue();
        } else if (rawDbId instanceof String str && !str.isBlank()) {
            try {
                this.dbId = Long.parseLong(str.trim());
            } catch (NumberFormatException e) {
                this.dbId = null;
            }
        }
    }

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
    public void setTypes(List<String> types) {
        this.types = types != null ? types : new ArrayList<>();
    }

    @JsonProperty("types")
    public void setTypesFromString(Object rawTypes) {
        if (rawTypes == null) {
            this.types = new ArrayList<>();
        } else if (rawTypes instanceof List<?> list) {
            this.types = list.stream().map(String::valueOf).toList();
        } else if (rawTypes instanceof String str) {
            if (str.isBlank()) {
                this.types = new ArrayList<>();
            } else {
                this.types = Arrays.stream(str.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        }
    }

    public Boolean getDining() { return isDining; }
    public void setDining(Boolean dining) { isDining = dining; }

    public Boolean getClosed() {
        return isClosed;
    }

    public void setClosed(Boolean closed) {
        isClosed = closed;
    }

    public VenueCategory getVenueCategory() { return venueCategory; }
    public void setVenueCategory(VenueCategory venueCategory) {
        this.venueCategory = venueCategory != null ? venueCategory : VenueCategory.RESTAURANT;
    }

    public void setVenueCategoryFromString(String categoryStr) {
        this.venueCategory = VenueCategory.fromKey(categoryStr);
    }

    public Boolean getVeganFriendly() { return veganFriendly; }
    public void setVeganFriendly(Boolean veganFriendly) { this.veganFriendly = veganFriendly; }

    public Boolean getVegetarianFriendly() { return vegetarianFriendly; }
    public void setVegetarianFriendly(Boolean vegetarianFriendly) { this.vegetarianFriendly = vegetarianFriendly; }

    public Boolean getGlutenFreeFriendly() { return glutenFreeFriendly; }
    public void setGlutenFreeFriendly(Boolean glutenFreeFriendly) { this.glutenFreeFriendly = glutenFreeFriendly; }

    public List<String> getDietaryTags() {
        return dietaryTags;
    }
    public void setDietaryTags(Object raw) {
        this.dietaryTags = parseCsvToList(raw);
    }

    public List<String> getHighlightedFoods() {
        return highlightedFoods;
    }
    public void setHighlightedFoods(Object raw) {
        this.highlightedFoods = parseCsvToList(raw);
    }

    public List<String> getAtmosphere() {
        return atmosphere;
    }
    public void setAtmosphere(Object raw) {
        this.atmosphere = parseCsvToList(raw);
    }

    public List<String> getServiceNotes() {
        return serviceNotes;
    }
    public void setServiceNotes(Object raw) {
        this.serviceNotes = parseCsvToList(raw);
    }

    public List<String> getCautions() {
        return cautions;
    }
    public void setCautions(Object raw) {
        this.cautions = parseCsvToList(raw);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("qdrant_id", qdrantId != null ? qdrantId : "");
        map.put("name", name);
        map.put("place_id", placeId);
        map.put("db_id", dbId != null ? String.valueOf(dbId) : "");
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

        map.put("is_dining", isDining != null ? isDining : true);
        map.put("is_closed", isClosed != null ? isClosed : false);
        map.put("venue_category", venueCategory != null ? venueCategory.getKey() : VenueCategory.RESTAURANT.getKey());
        map.put("is_vegan_friendly", Boolean.TRUE.equals(veganFriendly));
        map.put("is_vegetarian_friendly", Boolean.TRUE.equals(vegetarianFriendly));
        map.put("is_gluten_free_friendly", Boolean.TRUE.equals(glutenFreeFriendly));

        map.put("types", listToString(types));
        map.put("dietary_tags", listToString(dietaryTags));
        map.put("highlighted_foods", listToString(highlightedFoods));
        map.put("atmosphere", listToString(atmosphere));
        map.put("service_notes", listToString(serviceNotes));
        map.put("cautions", listToString(cautions));

        return map;
    }

    private String listToString(List<String> list) {
        return list != null
                ? String.join(", ", list)
                : "";
    }

    private List<String> parseCsvToList(Object raw) {
        if (raw == null) return new ArrayList<>();
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String str) {
            if (str.isBlank()) return new ArrayList<>();
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return new ArrayList<>();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // 1. Temel Tanımlayıcılar
        sb.append(String.format("'%s' (DB: %s, Place: %s) [%s]",
                name,
                dbId != null ? dbId : "-",
                placeId != null ? placeId : "-",
                venueCategory != null ? venueCategory.getKey() : "BELİRTİLMEMİŞ"));

        // 2. Yeme-İçme ve Puan Durumu
        sb.append(String.format(" | Dining: %s | Puan: %.1f",
                Boolean.TRUE.equals(isDining) ? "EVET" : "HAYIR",
                googleRating != null ? googleRating : 0.0));

        if (tastymapRating != null && tastymapRating > 0.0) {
            sb.append(String.format(" (TM: %.1f)", tastymapRating));
        }

        if (Boolean.TRUE.equals(isClosed)) {
            sb.append(" | [KAPALI MEKAN]");
        }

        // 3. Mesafe ve Arama Skorları (Sadece varsa basılır, indekslemede boş kalabalık yapmaz)
        if (distanceKm != null) {
            sb.append(String.format(" | Mesafe: %.2f km", distanceKm));
        }
        if (vectorScore != null && vectorScore > 0.0) {
            sb.append(String.format(" | Vektör: %.4f", vectorScore));
        }
        if (compositeScore != null && compositeScore > 0.0) {
            sb.append(String.format(" | Final: %.4f", compositeScore));
        }

        // 4. Diyet Özeti (Hap Bilgi)
        sb.append(String.format(" | Diyet: [Vegan: %s, Vej: %s, Gluten: %s]",
                Boolean.TRUE.equals(veganFriendly) ? "E" : "H",
                Boolean.TRUE.equals(vegetarianFriendly) ? "E" : "H",
                Boolean.TRUE.equals(glutenFreeFriendly) ? "E" : "H"));

        // 5. İçgörüler (Sadece dolu olanlar eklenir)
        if (dietaryTags != null && !dietaryTags.isEmpty()) {
            sb.append(" | Diyet Etiketleri: ").append(dietaryTags);
        }
        if (highlightedFoods != null && !highlightedFoods.isEmpty()) {
            sb.append(" | Lezzetler: ").append(highlightedFoods);
        }
        if (cautions != null && !cautions.isEmpty()) {
            sb.append(" | Uyarılar: ").append(cautions);
        }

        return sb.toString();
    }

    public String toPresentationMarkdown() {
        StringBuilder sb = new StringBuilder();

        sb.append("### **").append(name != null ? name : "Mekan").append("**");

        List<String> badges = new ArrayList<>();
        if (distanceKm != null) {
            badges.add(String.format("📍 %.1f km", distanceKm));
        }
        if (googleRating != null && googleRating > 0) {
            badges.add(String.format("%.1f ⭐", googleRating));
        }
        if (Boolean.TRUE.equals(isClosed)) {
            badges.add("Şu an Kapalı");
        }

        if (!badges.isEmpty()) {
            sb.append(" (").append(String.join(" | ", badges)).append(")");
        }
        sb.append("\n");

        if (highlightedFoods != null && !highlightedFoods.isEmpty()) {
            sb.append(String.format("  - **Öne Çıkan Lezzetler:** %s\n", String.join(", ", highlightedFoods)));
        }

        if (atmosphere != null && !atmosphere.isEmpty()) {
            sb.append(String.format("  - **Ortam:** %s\n", String.join(", ", atmosphere)));
        }

        if (serviceNotes != null && !serviceNotes.isEmpty()) {
            sb.append(String.format("  - **Hizmet ve Özellikler:** %s\n", String.join(", ", serviceNotes)));
        }

        if (dietaryTags != null && !dietaryTags.isEmpty()) {
            sb.append(String.format("  - **Diyet Seçenekleri:** %s\n", String.join(", ", dietaryTags)));
        }

        if (cautions != null && !cautions.isEmpty()) {
            sb.append(String.format("  - **Dikkat Edilmesi Gereken:** %s\n", String.join(", ", cautions)));
        }

        return sb.toString();
    }
}