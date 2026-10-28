package com.beem.TastyMap.rag.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceSummaryExtraction(
        @JsonProperty("is_dining") Boolean isDining,
        @JsonProperty("venue_category") String venueCategory,
        @JsonProperty("is_vegan_friendly") Boolean isVeganFriendly,
        @JsonProperty("is_vegetarian_friendly") Boolean isVegetarianFriendly,
        @JsonProperty("is_gluten_free_friendly") Boolean isGlutenFreeFriendly,
        @JsonProperty("dietary_tags") List<String> dietaryTags,
        @JsonProperty("highlighted_foods") List<String> highlightedFoods,
        @JsonProperty("atmosphere") List<String> atmosphere,
        @JsonProperty("service_notes") List<String> serviceNotes,
        @JsonProperty("cautions") List<String> cautions
) {
    public boolean safeIsDining() {
        return isDining != null ? isDining : true;
    }

    public boolean safeIsVeganFriendly() {
        return isVeganFriendly != null && isVeganFriendly;
    }

    public boolean safeIsVegetarianFriendly() {
        return isVegetarianFriendly != null && isVegetarianFriendly;
    }

    public boolean safeIsGlutenFreeFriendly() {
        return isGlutenFreeFriendly != null && isGlutenFreeFriendly;
    }
}