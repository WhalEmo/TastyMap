package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.mapsReview.ReviewUpdateEvent;
import com.beem.TastyMap.rag.service.RestaurantIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewIndexingListener {

    private final RestaurantIndexingService restaurantIndexingService;

    @Async
    @TransactionalEventListener
    public void handleReviewUpdate(ReviewUpdateEvent event) {
        log.info("Yeni yorum/güncelleme olayı yakalandı. Qdrant indeksi güncelleniyor. Place ID: {}", event.getPlaceId());
        // Event içindeki mekan bilgisi ve yorum listesi ile Qdrant güncellemesi yapılıyor
        restaurantIndexingService.indexPlaceWithReviews(event.getPlaceDetails(), event.getReviews());
    }
}