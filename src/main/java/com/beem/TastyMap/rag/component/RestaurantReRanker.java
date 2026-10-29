package com.beem.TastyMap.rag.component;

import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.document.VenueCategory;
import com.beem.TastyMap.rag.data.dto.UserHealthProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RestaurantReRanker {

    private static final Logger log = LoggerFactory.getLogger(RestaurantReRanker.class);

    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final Set<String> STOP_WORDS = Set.of(
            "bana", "guzel", "güzel", "mekan", "mekân", "oner", "öner", "oneri", "öneri",
            "tavsiye", "fena", "baska", "başka", "nerede", "nereye", "gidilir", "yiyebilirim",
            "yiyebilecegim", "yiyebileceğim", "yerler", "biri", "olan", "misin", "musun",
            "hakkinda", "hakkında", "nasıl", "nasil", "fiyat", "uygun"
    );

    public void rank(RestaurantDocument doc, String userQuery) {
        rank(doc, userQuery, null);
    }

    public void rank(RestaurantDocument doc, String userQuery, UserHealthProfile healthProfile) {
        RestaurantMetadata meta = doc.getMetadata();

        double vectorScore = meta.getVectorScore() != null ? meta.getVectorScore() : 0.5;

        double rating = meta.getGoogleRating() != null ? meta.getGoogleRating() : 3.0;
        double ratingScore = Math.min(Math.max(rating / 5.0, 0.0), 1.0);

        double distanceKm = meta.getDistanceKm() != null ? meta.getDistanceKm() : 10.0;
        double distanceScore = 1.0 / (1.0 + (distanceKm * 0.12));

        double richnessScore = 0.0;
        String content = doc.getRawContent();
        if (content.contains("Öne Çıkan Lezzetler:")) richnessScore += 0.5;
        if (content.contains("Ortam ve Ambiyans:")) richnessScore += 0.5;

        double relevanceBoost = 0.0;
        double healthBoost = 0.0;

        List<String> foods = meta.getHighlightedFoods() != null ? meta.getHighlightedFoods() : Collections.emptyList();
        List<String> atmosphere = meta.getAtmosphere() != null ? meta.getAtmosphere() : Collections.emptyList();

        if (!foods.isEmpty()) richnessScore += 0.5;
        if (!atmosphere.isEmpty()) richnessScore += 0.5;


        boolean isNonDining = Boolean.FALSE.equals(meta.getDining()) ||
                meta.getVenueCategory() == VenueCategory.NON_DINING;

        if (!isNonDining) {
            String lowerName = meta.getName() != null ? meta.getName().toLowerCase(TR) : "";
            List<String> foodsLower = foods.stream().map(f -> f.toLowerCase(TR)).toList();

            // A) Kullanıcı Sorgusuyla Mekan / Yemek Eşleşmesi
            if (userQuery != null && !userQuery.isBlank()) {
                String lowerQuery = userQuery.toLowerCase(TR);
                for (String rawWord : lowerQuery.split("\\s+")) {
                    String word = rawWord.replaceAll("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]", "").trim();

                    if (word.length() < 3 || STOP_WORDS.contains(word)) {
                        continue;
                    }

                    String altWord = DietarySafetyRegistry.WORD_ROOT_ALIASES.getOrDefault(word, word);

                    if (lowerName.contains(word) || lowerName.contains(altWord)) {
                        relevanceBoost = Math.max(relevanceBoost, 0.18);
                    } else if (foodsLower.stream().anyMatch(f -> f.contains(word) || f.contains(altWord))) {
                        relevanceBoost = Math.max(relevanceBoost, 0.14);
                    }
                }
            }

            // B) Sağlık ve Diyet Uyumu Kümülatif Boostu
            if (healthProfile != null && healthProfile.isRestricted()) {
                healthBoost = calculateHealthBoost(meta, lowerName, foodsLower, healthProfile);
            }
        }

        double composite = (vectorScore * 0.40)
                + (ratingScore * 0.30)
                + (distanceScore * 0.25)
                + (richnessScore * 0.05)
                + relevanceBoost
                + healthBoost;

        doc.setCompositeScore(composite);

        log.info("[Re-Rank] {} Boost: {} HealthBoost: {}", meta, relevanceBoost, healthBoost);
    }

    private String extractDishesSection(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        int start = content.indexOf("öne çıkan lezzetler:");
        if (start != -1) {
            int end = content.indexOf("\n", start);
            return end != -1 ? content.substring(start, end) : content.substring(start);
        }
        return "";
    }


    private double calculateHealthBoost(String lowerName, String lowerContent, String dishesSection, UserHealthProfile profile) {
        double boost = 0.0;

        if (profile.isVegan()) {
            boolean nameMatch = lowerName.contains("vegan") || lowerName.contains("bitkisel mutfak");
            boolean contentMatch = dishesSection.contains("vegan menü") ||
                    dishesSection.contains("%100 bitkisel") ||
                    lowerContent.contains("tamamen vegan");

            if (nameMatch) {
                boost = Math.max(boost, 0.35); // İsmi doğrudan Vegan olan mekan (Örn: Veganarsist)
            } else if (contentMatch) {
                boost = Math.max(boost, 0.20); // Menüsünde açıkça tescilli vegan bölümü olan
            }
        }

        // B) ÇÖLYAK / GLUTENSİZ PROFİLİ İÇİN
        if (profile.allergies() != null && profile.allergies().contains("gluten")) {
            boolean nameMatch = lowerName.contains("glutensiz") || lowerName.contains("gluten-free");
            boolean contentMatch = dishesSection.contains("glutensiz menü") ||
                    dishesSection.contains("çölyak dostu") ||
                    lowerContent.contains("ayrı glutensiz mutfak");

            if (nameMatch) {
                boost = Math.max(boost, 0.35); // Örn: 'Nane'i 7 Glutensiz cafe'
            } else if (contentMatch) {
                boost = Math.max(boost, 0.20);
            }
        }

        return boost;
    }

    private double calculateHealthBoost(
            RestaurantMetadata meta,
            String lowerName,
            List<String> foodsLower,
            UserHealthProfile profile
    ) {
        double totalBoost = 0.0;
        List<String> tagsLower = meta.getDietaryTags() != null
                ? meta.getDietaryTags().stream().map(t -> t.toLowerCase(TR)).toList()
                : Collections.emptyList();

        // 1. VEGAN / VEJETARYEN KATKISI
        if (profile.isVegan()) {
            boolean nameMatch = lowerName.contains("vegan") || lowerName.contains("bitkisel");
            boolean foodMatch = foodsLower.stream().anyMatch(f -> f.contains("vegan") || f.contains("bitkisel"));
            boolean tagMatch = tagsLower.stream().anyMatch(t -> t.contains("vegan"));

            if (nameMatch) {
                totalBoost += 0.25; // Doğrudan vegan mekan
            } else if (Boolean.TRUE.equals(meta.getVeganFriendly()) || foodMatch || tagMatch) {
                totalBoost += 0.15; // Menüsünde vegan seçenek olan
            }
        } else if (profile.isVegetarian() && Boolean.TRUE.equals(meta.getVegetarianFriendly())) {
            totalBoost += 0.12;
        }

        // 2. GLUTENSİZ / ÇÖLYAK KATKISI
        boolean hasGlutenRestriction = (profile.allergies() != null && profile.allergies().stream()
                .anyMatch(a -> a.equalsIgnoreCase("gluten") || a.equalsIgnoreCase("çölyak"))) ||
                profile.isCeliac();

        if (hasGlutenRestriction) {
            boolean nameMatch = lowerName.contains("glutensiz") || lowerName.contains("gluten-free");
            boolean foodMatch = foodsLower.stream().anyMatch(f -> f.contains("glutensiz") || f.contains("çölyak"));
            boolean tagMatch = tagsLower.stream().anyMatch(t -> t.contains("glutensiz"));

            if (nameMatch) {
                totalBoost += 0.25; // Örn: Nane'i 7 Glutensiz Cafe
            } else if (Boolean.TRUE.equals(meta.getGlutenFreeFriendly()) || foodMatch || tagMatch) {
                totalBoost += 0.15;
            }
        }

        // 3. DİYABET KATKISI (Şekersiz / Düşük Karbonhidratlı Seçenekler)
        if (profile.hasDiabetes()) {
            boolean hasSugarFreeOption = tagsLower.stream().anyMatch(t -> t.contains("şekersiz") || t.contains("diyabetik")) ||
                    foodsLower.stream().anyMatch(f -> f.contains("şekersiz") || f.contains("stevia") || f.contains("hurma"));

            if (hasSugarFreeOption) {
                totalBoost += 0.15; // Diyabete uygun alternatif sunuyor
            }
        }

        // 4. KORUYUCU TAVAN (CAP)
        // Çoklu kısıtları karşılayan mekan ödüllendirilir ama skor 0.45'i geçemez
        return Math.min(totalBoost, 0.45);
    }

}
