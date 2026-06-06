package com.beem.TastyMap.maps.service;


import com.beem.TastyMap.maps.GridUpdateEvent;
import com.beem.TastyMap.maps.data.*;
import com.beem.TastyMap.maps.data.geojson.FeatureCollection;
import com.beem.TastyMap.maps.entity.GridEntity;
import com.beem.TastyMap.maps.entity.GridStatus;
import com.beem.TastyMap.maps.entity.PhotoEntity;
import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.geo.GeoUtils;
import com.beem.TastyMap.maps.geo.GridCell;
import com.beem.TastyMap.maps.repository.PhotoRepo;
import com.beem.TastyMap.mapsReview.ReviewRepo;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.beem.TastyMap.maps.repository.GridRepo;
import com.beem.TastyMap.maps.repository.PlaceRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PlacesService {

    private final RedisCacheService redisService;
    private final GooglePlacesService googlePlacesService;
    private final EntityManager entityManager;
    private final PlaceRepo placeRepo;
    private final GridRepo gridRepo;
    private final PhotoRepo photoRepo;
    private final ReviewRepo reviewRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final PlaceMapper placeMapper;

    public PlacesService(
            RedisCacheService service,
            GooglePlacesService googlePlacesService,
            EntityManager entityManager,
            PlaceRepo placeRepo,
            GridRepo gridRepo,
            PhotoRepo photoRepo,
            ReviewRepo reviewRepo,
            ApplicationEventPublisher eventPublisher,
            PlaceMapper placeMapper
    ) {
        this.redisService = service;
        this.googlePlacesService = googlePlacesService;
        this.entityManager = entityManager;
        this.placeRepo = placeRepo;
        this.gridRepo = gridRepo;
        this.photoRepo = photoRepo;
        this.reviewRepo = reviewRepo;
        this.eventPublisher = eventPublisher;
        this.placeMapper = placeMapper;
    }

    @Cacheable(
            value = "places",
            key = "#placeId"
    )
    public PlaceEntity getPlaceByPlaceId(Long placeId){
        return placeRepo
                .findById(placeId)
                .orElseThrow(()-> new RuntimeException("Place not found"));
    }

    @Transactional
    public PlaceEntity getReferenceIfExists(Long id) {
        if (!placeRepo.existsById(id)) {
            throw new RuntimeException("Place not found");
        }
        return entityManager.getReference(PlaceEntity.class, id);
    }

    public PlacesResponse getPlaces(ScanRequest request){
        List<GridCell> gridCells = GeoUtils.gridCells(
                request.getLat(),
                request.getLng(),
                request.getRadius(),
                request.getKeywords()
        );

        List<PlaceResult> returnData = null;
        ArrayList<PlaceResult> results = new ArrayList<>();
        for(GridCell cell: gridCells){

            returnData = searchCacheToPlace(cell);

            if(returnData != null){
                results.addAll(returnData);
                continue;
            }

            returnData = searchPlaceDataBase(cell);

            if(returnData != null){
                results.addAll(returnData);
                continue;
            }

            returnData = searchPlaceGoogleAPI(cell);

            results.addAll(returnData);


        }

        FeatureCollection geoJson = placeMapper.convertToGeoJson(results);

        PlacesResponse response = new PlacesResponse();
        response.setResults(results);
        response.setGeoJson(geoJson);
        response.setStatus("Ok");

        return response;
    }

    @Transactional
    public PlaceDetailsResponse getPlaceDetails(String placeID){
        String key = RedisKeyGenerator.createPlaceDetailsKey(placeID);

        PlaceDetailsResult detailsList = searchCacheToPlaceDetails(key);

        if(detailsList != null){
            return mapToDetailsResult(detailsList);
        }

        detailsList = searchPlaceDetailsDataBase(placeID, key);

        if(detailsList != null){
            return mapToDetailsResult(detailsList);
        }

        detailsList = searchPlaceDetailsGoogleAPI(placeID, key);


        return mapToDetailsResult(detailsList);
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

    private List<PlaceResult> searchPlaceDataBase(GridCell cell){
        Optional<GridEntity> gridEntity = gridRepo.findByGridAndTypes(
                cell.getLat(),
                cell.getLng(),
                cell.getTypes()
        );

        GridEntity grid = gridEntity.orElse(null);

        //google api yönlendir
        if(grid == null){
            return null;
        }

        eventPublisher.publishEvent(new GridUpdateEvent(grid.getId(), cell));

        List<PlaceResult> placeResults = grid.getPlaces()
                .stream()
                .map(PlaceResult::fromEntity)
                .toList();

        cachePlaceResults(cell, placeResults);

        return placeResults;
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

        PlaceDetailsResult detailsResult = PlaceDetailsResult.fromEntity(entity);

        cachePlaceDetailsResults(key, detailsResult);

        return detailsResult;
    }

    @Transactional
    public List<PlaceResult> searchPlaceGoogleAPI(GridCell cell){

        List<PlaceResult> placeResults = fetchAndFilterPlaces(cell);

        markAPICounter(cell);

        GridEntity grid = getOrCreatedGrid(cell);

        if(placeResults.isEmpty()){
            markGridAsEmpty(grid);
            cachePlaceResults(cell, placeResults);
            return Collections.emptyList();
        }

        markGridAsHasData(grid);

        persistPlaces(grid, placeResults);

        cachePlaceResults(cell, placeResults);

        return placeResults;
    }

    public PlaceDetailsResult searchPlaceDetailsGoogleAPI(String placeId, String key){
        PlaceDetailsResponse response = googlePlacesService.getDetailPlaceInfo(placeId, key);

        if (!"OK".equals(response.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Google Places error: " + response.getStatus()
            );
        }
        PlaceDetailsResult details = response.getResult();

        PlaceEntity place = getOrCreatedOrUpdatePlace(details);

        cachePlaceDetailsResults(key, details);

        return details;
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
    private PlaceEntity getOrCreatedOrUpdatePlace(PlaceDetailsResult details){

        Optional<PlaceEntity> existingPlaceEntity = placeRepo.findByPlaceId(details.getPlace_id());

        if(existingPlaceEntity.isPresent()){
            PlaceEntity place = existingPlaceEntity.get();
            syncPlaceContent(place, details);
            return place;
        }

        Long gridId = gridRepo
                .findIdByGridLatAndLng(
                        GeoUtils.roundToGridCenter(details.getGeometry().getLocation().getLat()),
                        GeoUtils.roundToGridCenter(details.getGeometry().getLocation().getLng())
                )
                .orElseThrow(()-> new IllegalStateException("Grid not found"));

        GridEntity gridRef = entityManager.getReference(GridEntity.class, gridId);

        PlaceEntity newPlace = PlaceEntity.fromDetailsDto(details);
        syncPlaceContent(newPlace, details);
        newPlace.setGrid(gridRef);

        return placeRepo.save(newPlace);
    }

    @Transactional
    private void syncPlaceContent(PlaceEntity place, PlaceDetailsResult details){
        place.updateFromDetailsDto(details);

        if(details.getPhotos() != null){
            syncPlacePhotos(place, details.getPhotos());
        }
        if(details.getReviews() != null){
            syncPlaceReviews(place, details.getReviews());
        }
    }

    private void syncPlacePhotos(PlaceEntity place, List<Photo> photos){
        Set<String> existingPhotoRefs = photoRepo
                .findAllReferencesByPlaceId(place.getPlaceId())
                .stream()
                .map(ref -> ref.replaceAll("\\s", ""))
                .collect(Collectors.toSet());

        List<PhotoEntity> newPhotos = photos.stream()
                .filter(photo -> photo.getPhoto_reference() != null)
                .filter(photo -> {
                    String cleanRef = photo.getPhoto_reference().replaceAll("\\s", "");
                    return !existingPhotoRefs.contains(cleanRef);
                })
                .map(photo -> {
                    PhotoEntity newPhoto = new PhotoEntity();
                    newPhoto.setPhotoReference(photo.getPhoto_reference().replaceAll("\\s", ""));
                    newPhoto.setPlace(place);
                    newPhoto.setHeight(photo.getHeight());
                    newPhoto.setWidth(photo.getWidth());
                    return newPhoto;
                })
                .toList();
        if(!newPhotos.isEmpty()){
            for(PhotoEntity newPhoto: newPhotos){
                place.addPhoto(newPhoto);
            }
        }
    }

    private void syncPlaceReviews(PlaceEntity place, List<Review> reviews){
        Set<Long> existingReviewsCreated = reviewRepo.findAllCreatedAtsByPlaceId(place.getPlaceId());

        List<ReviewEntity> newReviews = reviews.stream()
                .filter(review -> {
                    return !existingReviewsCreated.contains(review.getTime());
                })
                .map(review -> {
                    return new ReviewEntity(
                            review.getAuthor_name(),
                            review.getRating(),
                            review.getText(),
                            review.getTime(),
                            place
                    );
                })
                .toList();
        if (!newReviews.isEmpty()){
            reviewRepo.saveAll(newReviews);
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
                null,
                3600
        );
    }

    private PlaceEntity mapToEntity(
            PlaceResult dto,
            GridEntity grid,
            Map<String, Long> existingIds) {

        PlaceEntity entity = PlaceEntity.fromDto(dto);
        entity.setGrid(grid);

        if(dto.getPhotos() != null){
            syncPlacePhotos(entity, dto.getPhotos());
        }

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
    
}
