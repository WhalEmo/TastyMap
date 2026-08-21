package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.maps.data.PlaceDetailsResult;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantIndexingService {

    private final VectorStore vectorStore;

    public void indexPlaceWithReviews(PlaceDetailsResult place, List<ReviewResult> reviews) {
        String placeId = place.getPlace_id();
        if (placeId == null || placeId.isBlank()) {
            log.warn("Place ID bulunamadığı için Qdrant indekslemesi atlandı.");
            return;
        }

        // 1. DÜZELTME: Eski kaydı silerek mükerrer (duplicate) kaydı engelleme
        try {
            vectorStore.delete(List.of(placeId));
        } catch (Exception e) {
            log.debug("Eski indeks bulunamadı veya silinirken bir uyarı alındı (Normal durum): {}", e.getMessage());
        }

        String typesText = (place.getTypes() != null) ? String.join(", ", place.getTypes()) : "";

        String reviewsCombined = "";
        if (reviews != null && !reviews.isEmpty()) {
            reviewsCombined = reviews.stream()
                    .map(ReviewResult::getContent)
                    .filter(content -> content != null && !content.isBlank())
                    .collect(Collectors.joining(" | "));
        }

        String openingHoursText = "Çalışma saatleri bilgisi yok.";
        boolean isOpenNow = false;

        if (place.getOpening_hours() != null) {
            isOpenNow = Boolean.TRUE.equals(place.getOpening_hours().getOpen_now());
            if (place.getOpening_hours().getWeekday_text() != null) {
                openingHoursText = String.join(", ", place.getOpening_hours().getWeekday_text());
            }
        }

        String textContent = String.format(
                "Mekan Adı: %s. Adres: %s. Türler: %s. Çalışma Saatleri: %s. Yorumlar ve İçerik: %s",
                place.getName(),
                place.getFormatted_address(),
                typesText,
                openingHoursText,
                reviewsCombined
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("place_id", placeId);
        metadata.put("rating", place.getRating() != null ? place.getRating() : 0.0);
        metadata.put("is_open_now", isOpenNow);

        // Document ID olarak place_id veriliyor
        Document doc = new Document(placeId, textContent, metadata);

        // Yenilenmiş mekan bilgisini ekleme
        vectorStore.add(List.of(doc));
        log.info("Restoran indeksi başarıyla güncellendi: {}", placeId);
    }
}