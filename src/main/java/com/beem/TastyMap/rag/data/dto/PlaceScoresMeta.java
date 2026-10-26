package com.beem.TastyMap.rag.data.dto;

import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record PlaceScoresMeta(
        Map<ScoreType, Double> scores
) implements Serializable {

    @JsonCreator
    public PlaceScoresMeta(@JsonProperty("scores") Map<ScoreType, Double> scores) {
        if (scores == null || scores.isEmpty()) {
            this.scores = Collections.emptyMap();
        } else {
            Map<ScoreType, Double> safeMap = new EnumMap<>(ScoreType.class);
            safeMap.putAll(scores);
            this.scores = Collections.unmodifiableMap(safeMap);
        }
    }

    public static PlaceScoresMeta empty() {
        return new PlaceScoresMeta(Collections.emptyMap());
    }

    public Double get(ScoreType type) {
        return scores.get(type);
    }

    public boolean has(ScoreType type) {
        return scores.containsKey(type) && scores.get(type) != null && scores.get(type) > 0;
    }

    /**
     * Enum'a ne eklenirse eklensin Markdown çıktısını dinamik üretir.
     */
    public String toMarkdown() {
        if (scores.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        scores.forEach((type, score) -> {
            if (score != null) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append(formatScoreLabel(type)).append(String.format(": %.1f/5", score));
            }
        });

        return sb.isEmpty() ? "" : "  - **Puan Detayları:** " + sb + "\n";
    }

    // Enum adını Türkçe okunabilir etikete çeviren yardımcı eşleme
    private String formatScoreLabel(ScoreType type) {
        return switch (type) {
            case OVERALL -> "Genel";
            case TASTE -> "Lezzet";
            case INTERIOR_DESIGN -> "Ortam/Tasarım";
            case SERVICE -> "Servis";
            case HOSPITALITY -> "İlgi/Güler Yüz";
            case PRICE_PERFORMANCE -> "Fiyat-Performans";
            case CLEANLINESS -> "Temizlik";
            case LOCATION -> "Konum";
            case WAITING_TIME -> "Hız/Bekleme";
        };
    }
}