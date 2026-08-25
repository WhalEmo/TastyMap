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

    /**
     * TEKİL İNDEKSLEME: Tek bir mekan eklendiğinde veya yorum güncellendiğinde çalışır.
     */
    public void indexPlaceWithReviews(PlaceDetailsResult place, List<ReviewResult> reviews) {
        String placeId = place.getPlace_id();
        if (placeId == null || placeId.isBlank()) {
            log.warn("Place ID bulunamadığı için Qdrant indekslemesi atlandı.");
            return;
        }

        try {
            vectorStore.delete(List.of(placeId));
        } catch (Exception e) {
            log.debug("Eski indeks bulunamadı veya silinirken bir uyarı alındı: {}", e.getMessage());
        }

        Document doc = createDocument(place, reviews);
        vectorStore.add(List.of(doc));

        log.info("Restoran indeksi başarıyla güncellendi: {}", placeId);
    }

    /**
     * TOPLU (BATCH) İNDEKSLEME: 100'erli paketler halinde Qdrant'a toplu veri gönderir.
     */
    public void indexPlacesBatch(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return;

        try {
            List<String> placeIds = documents.stream().map(Document::getId).toList();
            try {
                vectorStore.delete(placeIds);
            } catch (Exception e) {
                log.debug("Eski indeksler silinirken uyarı alındı: {}", e.getMessage());
            }

            // TEK BİR AĞ ISTEGIYLE TOPLU EKLEME
            vectorStore.add(documents);
            log.info("{} adet mekan topluca Qdrant'a eklendi.", documents.size());
        } catch (Exception e) {
            log.error("Batch indeksleme sırasında hata oluştu: {}", e.getMessage());
        }
    }

    /**
     * YARDIMCI METOD: PlaceDetailsResult nesnesini Qdrant Document nesnesine çevirir.
     */
    public Document createDocument(PlaceDetailsResult place, List<ReviewResult> reviews) {
        String placeId = place.getPlace_id();
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

        if (place.getGeometry() != null && place.getGeometry().getLocation() != null) {
            Map<String, Double> locationMap = new HashMap<>();
            locationMap.put("lat", place.getGeometry().getLocation().getLat());
            locationMap.put("lon", place.getGeometry().getLocation().getLng());
            metadata.put("location", locationMap);
        }

        return new Document(placeId, textContent, metadata);
    }
}