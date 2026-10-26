package com.beem.TastyMap.rag.component;

import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.dto.UserHealthProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class RestaurantReRanker {

    private static final Logger log = LoggerFactory.getLogger(RestaurantReRanker.class);

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

        String lowerContent = content.toLowerCase(Locale.forLanguageTag("tr"));
        String lowerName = meta.getName() != null ? meta.getName().toLowerCase(Locale.forLanguageTag("tr")) : "";
        String lowerQuery = userQuery != null ? userQuery.toLowerCase(Locale.forLanguageTag("tr")) : "";

        String dishesSection = extractDishesSection(lowerContent);

        boolean isNonDining = DietarySafetyRegistry.NON_DINING_EXCLUSIONS.stream().anyMatch(lowerName::contains);

        if (!isNonDining) {
            for (String rawWord : lowerQuery.split("\\s+")) {
                String word = rawWord.replaceAll("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]", "").trim();

                if (word.length() < 3 || STOP_WORDS.contains(word)) {
                    continue;
                }

                String altWord = DietarySafetyRegistry.WORD_ROOT_ALIASES.getOrDefault(word, word);

                if (lowerName.contains(word) || lowerName.contains(altWord)) {
                    relevanceBoost = Math.max(relevanceBoost, 0.18);
                }
                else if (!dishesSection.isEmpty() && (dishesSection.contains(word) || dishesSection.contains(altWord))) {
                    relevanceBoost = Math.max(relevanceBoost, 0.12);
                }
            }

            if (healthProfile != null && healthProfile.isRestricted()) {
                healthBoost = calculateHealthBoost(lowerName, lowerContent, dishesSection, healthProfile);
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

}
