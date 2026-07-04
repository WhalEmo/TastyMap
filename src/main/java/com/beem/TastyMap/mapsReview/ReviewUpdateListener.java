package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.beem.TastyMap.redis.RedisCacheService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ReviewUpdateListener {

    private final PlacesService placesService;
    private final ReviewRepo reviewRepo;
    private final RedisCacheService redisService;

    public ReviewUpdateListener(PlacesService placesService, ReviewRepo reviewRepo, RedisCacheService redisService) {
        this.placesService = placesService;
        this.reviewRepo = reviewRepo;
        this.redisService = redisService;
    }

    @Async
    @Transactional()
    @EventListener
    public void handleReviewUpdateEvent(ReviewUpdateEvent event) {
        String placeId = event.placeId();
        String updateKey = RedisKeyGenerator.createPlaceUpdateEventKey(placeId);

        if (redisService.exists(updateKey)) {
            return;
        }

        reviewRepo.findFirstBySourceAndPlace_PlaceIdOrderByCreatedAtDesc(
                ReviewSource.GOOGLE,
                placeId
        ).ifPresent(lastReview -> {
            if (isOlderThan30Days(lastReview.getCreatedAt())) {

                redisService.set(updateKey, "PROCESSING", 3600 * 24);

                String detailsKey = RedisKeyGenerator.createPlaceDetailsKey(placeId);

                System.out.println("handleReviewUpdateEvent tetiklendi: {}" + placeId);
                placesService.searchPlaceDetailsGoogleAPI(placeId, detailsKey);
            }
        });
    }

    private boolean isOlderThan30Days(Long createdAt) {
        if (createdAt == null) return true;
        Instant reviewCreatedAt = Instant.ofEpochMilli(createdAt);
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        return reviewCreatedAt.isBefore(thirtyDaysAgo);
    }

}
