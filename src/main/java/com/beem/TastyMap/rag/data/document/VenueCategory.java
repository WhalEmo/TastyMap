package com.beem.TastyMap.rag.data.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum VenueCategory {
    CAFE("CAFE", "Kafe & Kahve"),
    RESTAURANT("RESTAURANT", "Restoran"),
    MEAT_AND_GRILL("MEAT_AND_GRILL", "Et, Izgara, Döner & Kebap"),
    BAKERY_AND_DESSERT("BAKERY_AND_DESSERT", "Pastane, Tatlı & Fırın"),
    SEAFOOD("SEAFOOD", "Balık & Deniz Ürünleri"),
    FAST_FOOD("FAST_FOOD", "Fast Food & Sokak Lezzeti"),
    NON_DINING("NON_DINING", "Yeme-İçme Dışı (Oyun, Park, Kıraathane vb.)");

    private final String key;
    private final String description;

    VenueCategory(String key, String description) {
        this.key = key;
        this.description = description;
    }

    @JsonValue
    public String getKey() {
        return this.key;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * String değeri güvenli bir şekilde Enum'a dönüştürür.
     * LLM'den küçük harf, boşluk veya geçersiz değer gelse bile patlamaz.
     */
    @JsonCreator
    public static VenueCategory fromKey(String value) {
        if (value == null || value.isBlank()) {
            return RESTAURANT; // Güvenli varsayılan
        }

        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");

        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(normalized) || category.key.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(RESTAURANT);
    }

    public boolean isDining() {
        return this != NON_DINING;
    }
}