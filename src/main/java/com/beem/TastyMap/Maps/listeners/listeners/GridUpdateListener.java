package com.beem.TastyMap.maps.listeners.listeners;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.maps.listeners.events.GridUpdateEvent;
import com.beem.TastyMap.maps.entity.GridEntity;
import com.beem.TastyMap.maps.entity.GridStatus;
import com.beem.TastyMap.maps.geo.GridCell;
import com.beem.TastyMap.maps.repository.GridRepo;
import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GridUpdateListener.class);

    private final GridRepo gridRepo;
    private final PlacesService placesService;
    private final RedisCacheService redisCacheService;

    public GridUpdateListener(GridRepo gridRepo, PlacesService placesService, RedisCacheService redisCacheService) {
        this.gridRepo = gridRepo;
        this.placesService = placesService;
        this.redisCacheService = redisCacheService;
    }



    @Async
    @EventListener
    public void handleGridUpdateEvent(GridUpdateEvent event){
        String key = RedisKeyGenerator.createGridUpdateEventKey(event.cell());

        boolean isFirstTime = redisCacheService.setIfAbsent(key, "PROCESS", 3600 * 24);

        if(!isFirstTime){
            return;
        }

        try {
            GridEntity entity = gridRepo.findById(event.gridId())
                    .orElseThrow(() -> new CustomExceptions.NotFoundException("Grid entity not found"));

            LocalDateTime checkTime = entity.getLastScannedAt() != null
                    ? entity.getLastScannedAt()
                    : entity.getCreatedAt();

            if (checkTime != null && checkTime.isBefore(LocalDateTime.now().minusDays(30))) {
                log.info("Grid 30 günden eski, arka planda Google senkronizasyonu başlatılıyor: {}", event.cell().getGridKey());

                placesService.searchPlaceGoogleAPI(event.cell());
            }

        }
        catch (Exception e){
            log.error("Grid güncelleme eventinde hata oluştu: {}", e.getMessage());
            redisCacheService.delete(key);
        }

    }
}
