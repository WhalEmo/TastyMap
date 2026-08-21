package com.beem.TastyMap.maps;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.maps.entity.GridEntity;
import com.beem.TastyMap.maps.entity.GridStatus;
import com.beem.TastyMap.maps.geo.GridCell;
import com.beem.TastyMap.maps.repository.GridRepo;
import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class GridUpdateListener {

    private final GridRepo gridRepo;
    private final PlacesService placesService;
    private final RedisCacheService redisCacheService;

    public GridUpdateListener(GridRepo gridRepo, PlacesService placesService, RedisCacheService redisCacheService) {
        this.gridRepo = gridRepo;
        this.placesService = placesService;
        this.redisCacheService = redisCacheService;
    }



    @Async
    @Transactional
    @EventListener
    public void handleGridUpdateEvent(GridUpdateEvent event){
        String key = RedisKeyGenerator.createGridUpdateEventKey(event.cell());

        boolean isFirstTime = redisCacheService.setIfAbsent(key, "PROCESS", 3600 * 24);

        if(!isFirstTime){
            System.out.println("isFirstTime");
            return;
        }
        try {
            GridEntity entity = gridRepo.findById(event.gridId())
                    .orElseThrow(() -> new CustomExceptions.NotFoundException("Grid entity not found"));
            if(entity.getLastScannedAt() != null){
                System.out.println("getLastScannedAt");
                isOlderThanAndUpdate(entity, entity.getLastScannedAt(), event.cell());
            }
            else{
                System.out.println("getCreatedAt");
                isOlderThanAndUpdate(entity, entity.getCreatedAt(), event.cell());
            }
        }
        catch (Exception e){
            redisCacheService.delete(key);
            throw e;
        }

    }

    private void isOlderThanAndUpdate(GridEntity entity, LocalDateTime time, GridCell cell){
        System.out.println("******isOlderThanAndUpdate");
        Long lastScanned = toMillis(time);
        if(isOlderThan30Days(lastScanned)){
            List<?> places = placesService.searchPlaceGoogleAPI(cell);
            entity.setLastScannedAt(LocalDateTime.now());
            entity.setStatus(
                    places.isEmpty() ? GridStatus.EMPTY : GridStatus.HAS_DATA
            );
            gridRepo.save(entity);
        }
    }

    public Long toMillis(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;

        return localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private boolean isOlderThan30Days(Long createdAt) {
        if (createdAt == null) return true;
        Instant reviewCreatedAt = Instant.ofEpochMilli(createdAt);
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        return reviewCreatedAt.isBefore(thirtyDaysAgo);
    }
}
