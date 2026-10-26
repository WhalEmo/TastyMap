package com.beem.TastyMap.rag.component;

import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.dto.UserHealthProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
public class HealthSafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(HealthSafetyGuard.class);
    private static final Locale TR = Locale.forLanguageTag("tr");

    private static final Set<String> MEAT_EXCLUSIONS = DietarySafetyRegistry.MEAT_EXCLUSIONS;

    private static final Set<String> DIABETES_EXCLUSIONS = DietarySafetyRegistry.DIABETES_EXCLUSIONS;

    private static final Map<String, List<String>> ALLERGEN_KEYWORD_MAP = DietarySafetyRegistry.ALLERGEN_KEYWORD_MAP;

    private static final Set<String> DAIRY_AND_EGG_EXCLUSIONS = DietarySafetyRegistry.DAIRY_AND_EGG_EXCLUSIONS;

    private static final Set<String> PLANT_BASED_MODIFIERS = DietarySafetyRegistry.PLANT_BASED_MODIFIERS;

    private static final Pattern WORD_SPLIT_PATTERN = Pattern.compile("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]+");



    /**
     * Aday havuzunu kullanıcının sağlık kısıtlarına göre filtreler.
     * Güvenli olmayan adayları tamamen listeden düşürür.
     */
    public List<RestaurantDocument> filterSafePlaces(
            List<RestaurantDocument> candidates,
            UserHealthProfile healthProfile,
            String userQuery
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Kısıt yoksa sıfır maliyetle doğrudan geçir
        if (healthProfile == null || !healthProfile.isRestricted()) {
            return candidates;
        }

        String lowerQuery = userQuery != null ? userQuery.toLowerCase(TR) : "";
        List<RestaurantDocument> safeCandidates = new ArrayList<>(candidates.size());

        for (RestaurantDocument doc : candidates) {
            String rejectionReason = evaluateSafety(doc, healthProfile, lowerQuery);
            if (rejectionReason == null) {
                safeCandidates.add(doc);
            } else {
                log.info("🛡️ [SafetyGuard] '{}' elendi -> Neden: {}", doc.getMetadata().getName(), rejectionReason);
            }
        }

        log.info("🛡️ [SafetyGuard] Filtreleme bitti. Giren: {}, Kalan Güvenli: {}",
                candidates.size(), safeCandidates.size());

        return safeCandidates;
    }

    private String evaluateSafety(RestaurantDocument doc, UserHealthProfile profile, String lowerQuery) {
        RestaurantMetadata meta = doc.getMetadata();
        String placeName = meta.getName() != null ? meta.getName().toLowerCase(TR) : "";
        String content = doc.getRawContent() != null ? doc.getRawContent().toLowerCase(TR) : "";
        String dishesSection = extractDishesSection(content);

        Set<String> placeNameTokens = tokenize(placeName);
        Set<String> dishesTokens = tokenize(dishesSection);
        Set<String> combinedTokens = new HashSet<>(placeNameTokens);
        combinedTokens.addAll(dishesTokens);

        for (String nonDining : DietarySafetyRegistry.NON_DINING_EXCLUSIONS) {
            if (matchesPhraseOrToken(nonDining, placeName, placeNameTokens)) {
                return "Yeme-İçme Dışı Mekan (İhlal: '" + nonDining + "')";
            }
        }

        if (profile == null || !profile.isRestricted()) {
            return null;
        }

        boolean isDedicatedPlantBased = placeNameTokens.contains("vegan") ||
                placeNameTokens.contains("vejetaryen") ||
                placeNameTokens.contains("vejeteryan") ||
                placeNameTokens.contains("bitkisel");

        // A) VEGAN KONTROLÜ
        if (profile.isVegan()) {
            if (!isDedicatedPlantBased) {
                String meatViolation = findViolation(
                        placeName,
                        placeNameTokens,
                        dishesSection,
                        dishesTokens,
                        MEAT_EXCLUSIONS
                );
                if (meatViolation != null) {
                    return "VEGAN kısıtlaması (İhlal: '" + meatViolation + "')";
                }

                String dairyViolation = findViolation(
                        placeName,
                        placeNameTokens,
                        dishesSection,
                        dishesTokens,
                        DAIRY_AND_EGG_EXCLUSIONS
                );
                if (dairyViolation != null) {
                    return "VEGAN kısıtlaması (Süt/Yumurta İhlali: '" + dairyViolation + "')";
                }
            }
        }

        // B) VEJETARYEN KONTROLÜ (Vegan kuralından daha esnek ama saf sakatat/et elenir)
        else if (profile.isVegetarian()) {
            if (!isDedicatedPlantBased) {
                String meatViolation = findViolation(
                        placeName,
                        placeNameTokens,
                        dishesSection,
                        dishesTokens,
                        MEAT_EXCLUSIONS
                );
                if (meatViolation != null) {
                    return "VEJETARYEN kısıtlaması (İhlal: '" + meatViolation + "')";
                }
            }
        }

        // C) DİYABET KONTROLÜ (Kullanıcı açıkça tatlı aramadıysa tatlıcıları düşür)
        if (profile.hasDiabetes() && !isExplicitSweetSearch(lowerQuery)) {
            for (String sweetWord : DIABETES_EXCLUSIONS) {
                if (matchesPhraseOrToken(sweetWord, placeName, placeNameTokens)) {
                    return "DİYABET koruması (İhlal: Tatlı odaklı mekan '" + sweetWord + "')";
                }
            }
        }

        // D) ALERJEN KONTROLÜ
        if (profile.allergies() != null && !profile.allergies().isEmpty()) {
            for (String userAllergy : profile.allergies()) {

                List<String> riskyWords = ALLERGEN_KEYWORD_MAP.getOrDefault(userAllergy, List.of(userAllergy));
                for (String risk : riskyWords) {
                    String cleanRisk = risk.toLowerCase(TR);
                    if (matchesPhraseOrToken(cleanRisk, placeName + " " + dishesSection, combinedTokens)) {
                        return "ALERJİ koruması [" + userAllergy + "] (İhlal: '" + cleanRisk + "')";
                    }
                }
            }
        }

        return null; // Mekan güvenli
    }


    private String findViolation(String placeName, Set<String> nameTokens,
                                 String dishesSection, Set<String> dishesTokens,
                                 Set<String> exclusions) {
        for (String badWord : exclusions) {
            if (matchesPhraseOrToken(badWord, placeName, nameTokens)) {
                return badWord;
            }

            if (matchesPhraseOrToken(badWord, dishesSection, dishesTokens)) {
                // Koruyucu kontrolü: Eğer menüde "vegan/bitkisel" ifadesi geçiyorsa ihlal sayma
                boolean hasPlantProtection = dishesTokens.stream().anyMatch(PLANT_BASED_MODIFIERS::contains);
                if (!hasPlantProtection) {
                    return badWord;
                }
            }
        }
        return null;
    }

    private boolean matchesPhraseOrToken(String keyword, String fullText, Set<String> tokens) {
        if (keyword.contains(" ")) {
            // Çok kelimeli öbekler için kelime sınırlarıyla regex kontrolü
            String pattern = "(?U)(^|\\s|\\p{Punct})" + Pattern.quote(keyword) + "($|\\s|\\p{Punct})";
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(fullText).find();
        }
        // Tekil kelimeler için O(1) Token kontrolü (Substring false-positive'lerini tamamen sıfırlar)
        return tokens.contains(keyword);
    }

    private boolean isExplicitSweetSearch(String query) {
        if (query == null || query.isBlank()) return false;
        Set<String> queryTokens = tokenize(query);
        return queryTokens.contains("tatlı") || queryTokens.contains("tatli") ||
                queryTokens.contains("waffle") || queryTokens.contains("baklava") ||
                queryTokens.contains("pasta") || queryTokens.contains("dondurma");
    }

    private String extractDishesSection(String content) {
        if (content == null || content.isBlank()) return "";
        int start = content.indexOf("öne çıkan lezzetler:");
        if (start != -1) {
            int end = content.indexOf("\n", start);
            return end != -1 ? content.substring(start, end) : content.substring(start);
        }
        return "";
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(WORD_SPLIT_PATTERN.split(text))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}