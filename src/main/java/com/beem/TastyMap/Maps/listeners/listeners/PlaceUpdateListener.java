package com.beem.TastyMap.maps.listeners.listeners;

import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.maps.listeners.events.PlaceUpdateEvent;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.beem.TastyMap.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PlaceUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(PlaceUpdateListener.class);
    private static final long SYNC_THRESHOLD_DAYS = 30L;

    private final PlacesService placesService;
    private final RedisCacheService redisService;

    public PlaceUpdateListener(PlacesService placesService, RedisCacheService redisService) {
        this.placesService = placesService;
        this.redisService = redisService;
    }

    @Async
    @Transactional()
    @EventListener
    public void handleReviewUpdateEvent(PlaceUpdateEvent event) {
        log.info("Starting handleReviewUpdateEvent");

        String placeId = event.placeId();
        String updateLockKey = RedisKeyGenerator.createPlaceUpdateEventKey(placeId);

        Boolean isLockAcquired = redisService.setIfAbsent(updateLockKey, "PROCESSING", 3600 * 24);
        if (Boolean.FALSE.equals(isLockAcquired)) {
            log.info("Mekan güncellemesi halihazırda kilitli, atlandı: {}", placeId);
            return;
        }

        try {
            PlaceEntity place = placesService.findByPlaceId(placeId);

            if (place == null || isOlderThan30Days(place.getLastSyncedAt())) {
                log.info("Mekan verisi eski (30+ gün), Google senkronizasyonu tetikleniyor: {}", placeId);
                placesService.syncPlaceDetailsFromGoogle(placeId);
            } else {
                log.info("Mekan verisi taze, senkronizasyon atlandı: {}", placeId);
            }
        } catch (Exception e) {
            log.error("handlePlaceDetailsUpdateEvent hatası: {}, sebep: {}", placeId, e.getMessage(), e);
            redisService.delete(updateLockKey);
        }

    }

    private boolean isOlderThan30Days(LocalDateTime lastSyncedAt) {
        if (lastSyncedAt == null) {
            return true;
        }
        return lastSyncedAt.isBefore(LocalDateTime.now().minusDays(SYNC_THRESHOLD_DAYS));
    }

}
