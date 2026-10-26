package com.beem.TastyMap.rag.component;

import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.beem.TastyMap.rag.data.dto.PlaceScoresMeta;
import com.beem.TastyMap.rag.data.dto.PresentedPlaceSummary;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PlaceMetadataExtractor {

    public PresentedPlaceSummary fromDocument(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        String placeId = getString(meta, "place_id", "");
        String name = getString(meta, "name", "Bilinmeyen Mekan");
        Double googleRating = getDouble(meta, "google_rating", 0.0);
        Double tastymapRating = getDouble(meta, "tastymap_rating", 0.0);
        Boolean isOpen = getBoolean(meta, "is_open_now", null);
        Double distanceKm = getDouble(meta, "calculated_distance_km", null);

        // 1. Koordinatları Güvenli Çözümleme (location nested map desteği)
        Double lat = null;
        Double lon = null;
        if (meta.get("location") instanceof Map<?, ?> locMap) {
            Object latVal = locMap.get("lat");
            Object lonVal = locMap.get("lon");
            if (latVal instanceof Number n) lat = n.doubleValue();
            if (lonVal instanceof Number n) lon = n.doubleValue();
        } else {
            lat = getDouble(meta, "latitude", null);
            lon = getDouble(meta, "longitude", null);
        }

        Long dbId = null;
        if (meta.containsKey("db_id") && meta.get("db_id") != null) {
            try {
                dbId = Long.parseLong(meta.get("db_id").toString());
            } catch (Exception ignored) {}
        }

        // 2. Kategoriler
        List<String> categories = parseListOrString(meta.get("types"));

        // 3. Content İçerisindeki Başlıkları Ayrıştırma
        String content = doc.getContent() != null ? doc.getContent() : "";
        String address = getString(meta, "address", "");
        List<String> standoutDishes = new ArrayList<>();
        String ambiance = "";
        String warnings = "";
        StringBuilder reviewsBuilder = new StringBuilder();

        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Adres:")) {
                if (address.isBlank()) {
                    address = trimmed.replace("Adres:", "").trim();
                }
            } else if (trimmed.startsWith("Öne Çıkan Lezzetler:")) {
                String dishesStr = trimmed.replace("Öne Çıkan Lezzetler:", "").trim();
                standoutDishes = Arrays.stream(dishesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
            } else if (trimmed.startsWith("Ortam ve Ambiyans:")) {
                ambiance = trimmed.replace("Ortam ve Ambiyans:", "").trim();
            } else if (trimmed.startsWith("Dikkat Edilmesi Gerekenler:")) {
                warnings = trimmed.replace("Dikkat Edilmesi Gerekenler:", "").trim();
            } else if (trimmed.startsWith("Yorum Notları:") || trimmed.startsWith("Müşteri Yorumları:")) {
                String rev = trimmed.replaceAll("^(Yorum Notları:|Müşteri Yorumları:)", "").trim();
                if (!reviewsBuilder.isEmpty()) reviewsBuilder.append(" | ");
                reviewsBuilder.append(rev);
            }
        }

        String highlightReviews = reviewsBuilder.toString();
        if (highlightReviews.length() > 300) {
            highlightReviews = highlightReviews.substring(0, 300) + "...";
        }

        PlaceScoresMeta scores = extractScoresFromContent(content);

        return new PresentedPlaceSummary(
                doc.getId(),
                placeId,
                dbId,
                name,
                address,
                lat,
                lon,
                distanceKm,
                googleRating,
                tastymapRating,
                isOpen,
                categories,
                standoutDishes,
                ambiance,
                warnings,
                scores,
                highlightReviews
        );
    }

    private List<String> parseListOrString(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        } else if (obj instanceof String str && !str.isBlank()) {
            return Arrays.stream(str.split(",")).map(String::trim).toList();
        }
        return List.of();
    }

    private PlaceScoresMeta extractScoresFromContent(String content) {
        if (content == null || content.isBlank()) {
            return PlaceScoresMeta.empty();
        }
        Map<ScoreType, Double> scoreMap = new EnumMap<>(ScoreType.class);
        for (ScoreType type : ScoreType.values()) {
            Double score = parseScoreForType(content, type.name());
            if (score != null) {
                scoreMap.put(type, score);
            }
        }
        return scoreMap.isEmpty() ? PlaceScoresMeta.empty() : new PlaceScoresMeta(scoreMap);
    }

    private Double parseScoreForType(String content, String typeName) {
        String token = typeName + ":";
        int idx = content.indexOf(token);
        if (idx == -1) return null;
        try {
            int start = idx + token.length();
            int end = content.indexOf("/5", start);
            if (end != -1 && (end - start) < 10) {
                return Double.parseDouble(content.substring(start, end).trim());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getString(Map<String, Object> meta, String key, String def) {
        Object val = meta.get(key);
        return val != null ? val.toString() : def;
    }

    private Double getDouble(Map<String, Object> meta, String key, Double def) {
        Object val = meta.get(key);
        if (val == null) return def;
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private Boolean getBoolean(Map<String, Object> meta, String key, Boolean def) {
        Object val = meta.get(key);
        if (val == null) return def;
        try {
            return Boolean.parseBoolean(val.toString());
        } catch (Exception e) {
            return def;
        }
    }
}