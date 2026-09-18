package com.beem.TastyMap.maps.service;


import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.maps.listeners.events.GridUpdateEvent;
import com.beem.TastyMap.maps.data.*;
import com.beem.TastyMap.maps.data.geojson.FeatureCollection;
import com.beem.TastyMap.maps.data.google.GooglePlaceDetailsDto;
import com.beem.TastyMap.maps.data.google.GooglePlaceDetailsResponse;
import com.beem.TastyMap.maps.entity.GridEntity;
import com.beem.TastyMap.maps.entity.GridStatus;
import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.geo.GeoUtils;
import com.beem.TastyMap.maps.geo.GridCell;
import com.beem.TastyMap.maps.listeners.events.PlaceUpdateEvent;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.repository.ReviewQueryRepository;
import com.beem.TastyMap.mapsReview.repository.ReviewRepo;
import com.beem.TastyMap.mapsReview.data.response.UserReviewSummaryDto;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.stats.StatsService;
import com.beem.TastyMap.stats.data.response.PlaceStatsDto;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.beem.TastyMap.maps.repository.GridRepo;
import com.beem.TastyMap.maps.repository.PlaceRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PlacesService {

    private static final Logger log = LoggerFactory.getLogger(PlacesService.class);

    private final RedisCacheService redisService;
    private final GooglePlacesService googlePlacesService;
    private final EntityManager entityManager;
    private final PlaceRepo placeRepo;
    private final GridRepo gridRepo;
    private final ReviewRepo reviewRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final PlaceMapper placeMapper;
    private final ReviewQueryRepository reviewQueryRepository;
    private final StatsService statsService;
    private final TransactionTemplate transactionTemplate;

    public PlacesService(
            RedisCacheService service,
            GooglePlacesService googlePlacesService,
            EntityManager entityManager,
            PlaceRepo placeRepo,
            GridRepo gridRepo,
            ReviewRepo reviewRepo,
            ApplicationEventPublisher eventPublisher,
            PlaceMapper placeMapper,
            ReviewQueryRepository reviewQueryRepository,
            StatsService statsService,
            TransactionTemplate transactionTemplate
    ) {
        this.redisService = service;
        this.googlePlacesService = googlePlacesService;
        this.entityManager = entityManager;
        this.placeRepo = placeRepo;
        this.gridRepo = gridRepo;
        this.reviewRepo = reviewRepo;
        this.eventPublisher = eventPublisher;
        this.placeMapper = placeMapper;
        this.reviewQueryRepository = reviewQueryRepository;
        this.statsService = statsService;
        this.transactionTemplate = transactionTemplate;
    }


    @Transactional(readOnly = true)
    public PlaceEntity getReferenceIfExists(String placeId) {
        if (placeId == null || placeId.trim().isEmpty()) {
            throw new CustomExceptions.NotFoundException("Place identifier cannot be empty");
        }

        return placeRepo.findByPlaceId(placeId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Place not found with placeId: " + placeId));
    }


    @Transactional
    public PlaceDetailsResponse getPlaceDetails(String placeID, Long userId){
        String key = RedisKeyGenerator.createPlaceDetailsKey(placeID);

        PlaceDetailsResult detailsList = searchCacheToPlaceDetails(key);

        if(detailsList == null){
            detailsList = searchPlaceDetailsDataBase(placeID, key);
        }

        if(detailsList == null){
            detailsList = searchPlaceDetailsGoogleAPI(placeID, key);
        }

        attachUserReviewIfExists(userId, detailsList);

        return mapToDetailsResult(detailsList);
    }

    public PlacesResponse getPlaces(ScanRequest request) {
        List<GridCell> gridCells = GeoUtils.gridCells(
                request.getLat(),
                request.getLng(),
                request.getRadius(),
                request.getKeywords()
        );

        log.info("Tarama başladı. Toplam hücre sayısı: {}", gridCells.size());

        List<PlaceResult> allResults = Flux.fromIterable(gridCells)
                .flatMap(this::resolveSingleCellAsync, 2)
                .flatMap(Flux::fromIterable)
                .collectList()
                .block();

        if (allResults == null) {
            allResults = Collections.emptyList();
        }

        // Tekilleştirme (Deduplication): Farklı grid hücrelerinden taşan aynı place_id'leri teke indir
        Map<String, PlaceResult> uniqueMap = new LinkedHashMap<>();
        for (PlaceResult place : allResults) {
            if (place.getPlace_id() != null) {
                uniqueMap.putIfAbsent(place.getPlace_id(), place);
            }
        }
        List<PlaceResult> distinctResults = new ArrayList<>(uniqueMap.values());

        FeatureCollection geoJson = placeMapper.convertToGeoJson(distinctResults);

        PlacesResponse response = new PlacesResponse();
        response.setResults(distinctResults);
        response.setGeoJson(geoJson);
        response.setStatus("Ok");

        log.info("Tarama tamamlandı. Benzersiz mekan sayısı: {}", distinctResults.size());
        return response;
    }

    private Mono<List<PlaceResult>> resolveSingleCellAsync(GridCell cell) {
        log.info("Grid key: {}", cell.getGridKey());
        List<PlaceResult> cached = searchCacheToPlace(cell);
        if (cached != null) {
            return Mono.just(cached);
        }

        // 2. Veritabanı Kontrolü (Bloklamayan I/O thread'inde çalıştırılır)
        return Mono.fromCallable(() -> Optional.ofNullable(searchPlaceDataBase(cell)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optResults -> {
                    if (optResults.isPresent()) {
                        return Mono.just(optResults.get());
                    }

                    log.info("Grid DB'de yok veya bayatlamış, Google'a gidiliyor: {}", cell.getGridKey());
                    return fetchFromGoogleAndPersistAsync(cell);
                });
    }

    private Mono<List<PlaceResult>> fetchFromGoogleAndPersistAsync(GridCell cell) {
        return googlePlacesService.getNearbyFoodPlacesAsync(cell, cell.getGridKey())
                .publishOn(Schedulers.boundedElastic()) // DB yazma işlemi için I/O thread'ine geç
                .map(googleResponse -> {
                    if (googleResponse == null || googleResponse.getResults() == null) {
                        return Collections.<PlaceResult>emptyList();
                    }

                    // Mekanları filtrele
                    List<PlaceResult> filtered = googleResponse.getResults().stream()
                            .filter(place -> isPlaceInGrid(cell, place))
                            .collect(Collectors.toCollection(ArrayList::new));

                    // DB ve Cache kayıt işlemini TRANSACTION ile yap (HTTP çağrısı bittiği için güvenli)
                    savePlacesToDbAndCache(cell, filtered);

                    return filtered;
                })
                .defaultIfEmpty(Collections.emptyList());
    }

    public void savePlacesToDbAndCache(GridCell cell, List<PlaceResult> placeResults) {
        transactionTemplate.executeWithoutResult(status -> {
            markAPICounter(cell);
            GridEntity grid = getOrCreatedGrid(cell);

            grid.setLastScannedAt(LocalDateTime.now());

            if (placeResults.isEmpty()) {
                grid.setStatus(GridStatus.EMPTY);
                gridRepo.save(grid);
                return;
            }

            grid.setStatus(GridStatus.HAS_DATA);
            gridRepo.save(grid);
            persistPlaces(grid, placeResults);
        });
        if (placeResults.isEmpty()) {
            cachePlaceResults(cell, Collections.emptyList());
        } else {
            cachePlaceResults(cell, placeResults);
        }
    }

    private List<PlaceResult> searchCacheToPlace(GridCell cell){
        return redisService.getWithSlidingTTL(
                cell.getGridKey(),
                new TypeReference<List<PlaceResult>>() {},
                3600
        );
    }

    private PlaceDetailsResult searchCacheToPlaceDetails(String key){
        return redisService.getWithSlidingTTL(
                key,
                new TypeReference<PlaceDetailsResult>(){},
                3600 * 24
        );
    }

    @Transactional(readOnly = true)
    private List<PlaceResult> searchPlaceDataBase(GridCell cell){
        Optional<GridEntity> gridOpt = gridRepo.findByGridLatAndLng(
                cell.getLat(),
                cell.getLng()
        );

        if (gridOpt.isEmpty()) {
            return null;
        }

        GridEntity grid = gridOpt.get();

        List<PlaceEntity> placeEntities = placeRepo.findPlacesByGridAndTypes(
                cell.getLat(),
                cell.getLng(),
                cell.getTypes()
        );

        if (!placeEntities.isEmpty()) {
            eventPublisher.publishEvent(new GridUpdateEvent(grid.getId(), cell));

            List<PlaceResult> placeResults = placeEntities.stream()
                    .map(PlaceResult::fromEntity)
                    .toList();

            cachePlaceResults(cell, placeResults);
            return placeResults;
        }

        if (grid.getStatus() == GridStatus.EMPTY) {
            if (grid.getLastScannedAt() != null &&
                    grid.getLastScannedAt().isAfter(LocalDateTime.now().minusDays(30))) {
                cachePlaceResults(cell, Collections.emptyList());
                return Collections.emptyList();
            }
        }


        return null;
    }

    public PlaceEntity save(PlaceEntity place) {
        return placeRepo.save(place);
    }


    public void evictPlaceCache(String placeId) {
        if (placeId == null || placeId.trim().isEmpty()) {
            return;
        }

        String detailKey = RedisKeyGenerator.createPlaceDetailsKey(placeId);

        redisService.delete(detailKey);
    }

    private PlaceDetailsResult searchPlaceDetailsDataBase(String placeId, String key){
        Optional<PlaceEntity> placeEntity = placeRepo.findByPlaceId(placeId);

        PlaceEntity entity = placeEntity.orElse(null);

        if(entity == null){
            return null;
        }
        else if(entity.getFormattedAddress() == null){
            return null;
        }

        eventPublisher.publishEvent(new PlaceUpdateEvent(entity.getPlaceId()));

        PlaceDetailsResult detailsResult = PlaceDetailsResult.fromEntity(entity);

        List<ReviewResult> reviews = reviewQueryRepository.findReviews(entity.getId(), 0, 5);
        detailsResult.setReviews(reviews);

        PlaceStatsDto stats = statsService.calculatePlaceStats(
                entity.getId(),
                entity.getTastyMapRating(),
                entity.getTastyMapReviewCount()
        );

        detailsResult.setStats(stats);

        cachePlaceDetailsResults(key, detailsResult);

        return detailsResult;
    }

    @Transactional
    public List<PlaceResult> searchPlaceGoogleAPI(GridCell cell){

        List<PlaceResult> placeResults = fetchAndFilterPlaces(cell);

        savePlacesToDbAndCache(cell, placeResults);
        return placeResults;
    }

    public PlaceDetailsResult searchPlaceDetailsGoogleAPI(String placeId, String key){
        log.info("Google Places Details API çağrılıyor: {}", placeId);

        GooglePlaceDetailsResponse response = googlePlacesService.getDetailPlaceInfo(placeId, key);

        if (!"OK".equals(response.getStatus())) {
            log.error("Google Places Details API hatası: {} - {}", placeId, response.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Google Places error: " + response.getStatus()
            );
        }
        GooglePlaceDetailsDto dto = response.getResult();

        PlaceEntity place = getOrCreatedOrUpdatePlace(dto);

        PlaceDetailsResult details = PlaceDetailsResult.fromEntity(place);

        List<ReviewResult> reviews = reviewQueryRepository.findReviews(place.getId(), 0, 5);
        details.setReviews(reviews);

        PlaceStatsDto stats = statsService.calculatePlaceStats(
                place.getId(),
                place.getTastyMapRating(),
                place.getTastyMapReviewCount()
        );
        details.setStats(stats);

        cachePlaceDetailsResults(key, details);

        return details;
    }


    @Transactional
    public void syncPlaceDetailsFromGoogle(String placeId) {
        String dummyKey = "SYNC_BG_" + placeId;
        log.info("Google Place Details senkronizasyonu başlatıldı: {}", placeId);

        GooglePlaceDetailsResponse response = googlePlacesService.getDetailPlaceInfo(placeId, dummyKey);
        if (!"OK".equals(response.getStatus())) {
            log.warn("Google API senkronizasyon yanıtı başarısız: {} - {}", placeId, response.getStatus());
            return;
        }

        GooglePlaceDetailsDto dto = response.getResult();
        PlaceEntity place = getOrCreatedOrUpdatePlace(dto);

        place.setLastSyncedAt(LocalDateTime.now());
        placeRepo.save(place);

        evictPlaceCache(placeId);
        log.info("Google Place Details senkronizasyonu tamamlandı: {}", placeId);
    }


    private List<PlaceResult> fetchAndFilterPlaces(GridCell cell){
        PlacesResponse googleResponse = googlePlacesService.getNearbyFoodPlaces(cell, cell.getGridKey());

        return googleResponse.getResults()
                .stream()
                .filter(place -> isPlaceInGrid(cell, place))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isPlaceInGrid(GridCell cell,PlaceResult placeResult){
        BigDecimal gridLat = GeoUtils
                .roundToGridCenter(placeResult.getGeometry().getLocation().getLat());
        BigDecimal gridLng = GeoUtils
                .roundToGridCenter(placeResult.getGeometry().getLocation().getLng());
        return cell.getLat().compareTo(gridLat) == 0
                && cell.getLng().compareTo(gridLng) == 0;
    }

    private GridEntity getOrCreatedGrid(GridCell cell){
        return gridRepo
                .findByGridLatAndLng(
                        cell.getLat(),
                        cell.getLng()
                )
                .orElseGet(()->{
                    GridEntity entity = new GridEntity();
                    entity.setStatus(GridStatus.EMPTY);
                    entity.setCenterLat(cell.getLat());
                    entity.setCenterLng(cell.getLng());
                    return gridRepo.save(entity);
                });
    }
    private PlaceEntity getOrCreatedOrUpdatePlace(GooglePlaceDetailsDto details){

        Optional<PlaceEntity> existingPlaceEntity = placeRepo.findByPlaceId(details.getPlace_id());

        if(existingPlaceEntity.isPresent()){
            PlaceEntity place = existingPlaceEntity.get();
            syncPlaceContent(place, details);
            return place;
        }

        BigDecimal centerLat = GeoUtils.roundToGridCenter(details.getGeometry().getLocation().getLat());
        BigDecimal centerLng = GeoUtils.roundToGridCenter(details.getGeometry().getLocation().getLng());

        GridEntity gridEntity = gridRepo
                .findByGridLatAndLng(centerLat, centerLng)
                .orElseGet(() -> {
                    GridEntity g = new GridEntity();
                    g.setStatus(GridStatus.HAS_DATA);
                    g.setCenterLat(centerLat);
                    g.setCenterLng(centerLng);
                    g.setLastScannedAt(LocalDateTime.now());
                    return gridRepo.save(g);
                });


        PlaceEntity newPlace = PlaceEntity.fromDetailsDto(details);

        newPlace.setGrid(gridEntity);
        newPlace = placeRepo.saveAndFlush(newPlace);


        syncPlaceContent(newPlace, details);

        return newPlace;
    }

    @Transactional
    private void syncPlaceContent(PlaceEntity place, GooglePlaceDetailsDto details){

        if (details.getGeometry() != null && details.getGeometry().getLocation() != null) {
            double newLat = details.getGeometry().getLocation().getLat();
            double newLng = details.getGeometry().getLocation().getLng();

            // Yeni koordinatların ait olduğu grid merkezini hesapla
            BigDecimal targetGridLat = GeoUtils.roundToGridCenter(newLat);
            BigDecimal targetGridLng = GeoUtils.roundToGridCenter(newLng);

            // Eğer mekanın şu an bağlı olduğu grid, yeni koordinatların gridiyle uyuşmuyorsa taşı
            GridEntity currentGrid = place.getGrid();
            boolean gridChanged = currentGrid == null
                    || currentGrid.getCenterLat().compareTo(targetGridLat) != 0
                    || currentGrid.getCenterLng().compareTo(targetGridLng) != 0;

            if (gridChanged) {
                if (currentGrid != null) {
                    String oldGridPattern = RedisKeyGenerator.createGridPattern(
                            currentGrid.getCenterLat(),
                            currentGrid.getCenterLng()
                    );
                    redisService.deleteByPattern(oldGridPattern);
                }

                GridEntity newGrid = gridRepo.findByGridLatAndLng(targetGridLat, targetGridLng)
                        .orElseGet(() -> {
                            GridEntity g = new GridEntity();
                            g.setStatus(GridStatus.HAS_DATA);
                            g.setCenterLat(targetGridLat);
                            g.setCenterLng(targetGridLng);
                            g.setLastScannedAt(LocalDateTime.now());
                            return gridRepo.save(g);
                        });

                place.setGrid(newGrid);
                log.info("Mekan konumu değiştiği için grid güncellendi: {} -> Yeni Grid: [{}, {}]",
                        place.getName(), targetGridLat, targetGridLng);
            }
        }

        place.updateFromDetailsDto(details);
        place = placeRepo.save(place);

        if(details.getReviews() != null){
            syncPlaceReviews(place, details.getReviews());
        }
    }


    private void syncPlaceReviews(PlaceEntity place, List<Review> reviews){
        if (reviews == null || reviews.isEmpty() || place.getId() == null) return;


        Set<Long> existingTimes = reviewRepo.findAllTimesByPlaceDbId(place.getId());

        List<ReviewEntity> newReviews = reviews.stream()
                .filter(review -> review.time() != null && !existingTimes.contains(review.time()))
                .map(review -> {
                    return new ReviewEntity(
                            review.authorName(),
                            review.rating(),
                            review.text(),
                            review.time(),
                            place
                    );
                })
                .toList();
        if (!newReviews.isEmpty()){
            try {
                log.info("{} mekanı için {} yeni Google yorumu kaydediliyor.", place.getPlaceId(), newReviews.size());
                reviewRepo.saveAllAndFlush(newReviews);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("{} mekanı için eşzamanlı yorum kaydı atlandı (Unique Constraint): {}", place.getPlaceId(), e.getMessage());
            }
        }
    }

    private void persistPlaces(GridEntity grid, List<PlaceResult> results) {

        Set<String> placeIds = results.stream()
                .map(PlaceResult::getPlace_id)
                .collect(Collectors.toSet());

        Map<String, Long> existingIds = placeRepo
                .findIdsByPlaceIds(placeIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        List<PlaceEntity> entities = results.stream()
                .map(dto -> mapToEntity(dto, grid, existingIds))
                .collect(Collectors.toCollection(ArrayList::new));

        placeRepo.saveAll(entities);
    }


    private void attachUserReviewIfExists(Long userId, PlaceDetailsResult details) {
        if (userId == null || details == null) {
            if (details != null) {
                details.setUserReview(null);
            }
            return;
        }

        reviewRepo.findFirstByPlace_PlaceIdAndUserIdAndParentIsNullAndStatusOrderByCreatedAtDesc(
                details.getPlace_id(),
                userId,
                com.beem.TastyMap.mapsReview.enums.ReviewStatus.APPROVED
        ).ifPresentOrElse(
                reviewEntity -> details.setUserReview(UserReviewSummaryDto.fromEntity(reviewEntity)),
                () -> details.setUserReview(null)
        );
    }



    private void markGridAsEmpty(GridEntity grid) {
        grid.setStatus(GridStatus.EMPTY);
        gridRepo.save(grid);
    }

    private void markGridAsHasData(GridEntity grid) {
        grid.setStatus(GridStatus.HAS_DATA);
        gridRepo.save(grid);
    }


    private void cachePlaceResults(GridCell cell, List<PlaceResult> results) {
        redisService.set(
                cell.getGridKey(),
                results,
                3600
        );
    }
    private void cachePlaceDetailsResults(String key, PlaceDetailsResult detailsResult) {
        redisService.set(
                key,
                detailsResult,
                3600 * 24
        );
    }

    private void markAPICounter(GridCell cell){
        redisService.set(
                cell.getGridKey() + ":API",
                "CALLED",
                3600
        );
    }

    private PlaceEntity mapToEntity(
            PlaceResult dto,
            GridEntity grid,
            Map<String, Long> existingIds) {

        PlaceEntity entity = PlaceEntity.fromDto(dto);
        entity.setGrid(grid);


        Long existingId = existingIds.get(entity.getPlaceId());
        if (existingId != null) {
            entity.setId(existingId);
        }

        return entity;
    }

    private PlaceDetailsResponse mapToDetailsResult(PlaceDetailsResult detailsResult){
        PlaceDetailsResponse response = new PlaceDetailsResponse();
        response.setResult(detailsResult);
        response.setStatus("Ok");
        return response;
    }

    public PlaceEntity findById(Long id) {
        return placeRepo.findById(id)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Mekan bulunamadı. ID: " + id));
    }

    public PlaceEntity findByPlaceId(String placeId) {
        return placeRepo.findByPlaceId(placeId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Mekan bulunamadı. ID: " + placeId));
    }
    
}
