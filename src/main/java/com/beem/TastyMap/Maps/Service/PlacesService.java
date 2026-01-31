package com.beem.TastyMap.Maps.Service;


import com.beem.TastyMap.Maps.Data.*;
import com.beem.TastyMap.Maps.Entity.GridEntity;
import com.beem.TastyMap.Maps.Entity.GridStatus;
import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Geo.GeoUtils;
import com.beem.TastyMap.Maps.Geo.GridCell;
import com.beem.TastyMap.Maps.Redis.RedisKeyGenerator;
import com.beem.TastyMap.Maps.Repository.GridRepo;
import com.beem.TastyMap.Maps.Repository.PlaceRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import org.springframework.cache.annotation.Cacheable;
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

    public PlacesService(RedisCacheService service, GooglePlacesService googlePlacesService, EntityManager entityManager, PlaceRepo placeRepo, GridRepo gridRepo) {
        this.redisService = service;
        this.googlePlacesService = googlePlacesService;
        this.entityManager = entityManager;
        this.placeRepo = placeRepo;
        this.gridRepo = gridRepo;
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

        PlacesResponse response = new PlacesResponse();
        response.setResults(results);
        response.setStatus("Ok");

        return response;
    }

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
    private List<PlaceResult> searchPlaceGoogleAPI(GridCell cell){

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

    private PlaceDetailsResult searchPlaceDetailsGoogleAPI(String placeId, String key){
        PlaceDetailsResponse response = googlePlacesService.getDetailPlaceInfo(placeId);

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
        PlacesResponse googleResponse = googlePlacesService.getNearbyFoodPlaces(cell);

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
        Long gridId = gridRepo
                .findIdByGridLatAndLng(
                        GeoUtils.roundToGridCenter(details.getGeometry().getLocation().getLat()),
                        GeoUtils.roundToGridCenter(details.getGeometry().getLocation().getLng())
                )
                .orElseThrow(()-> new IllegalStateException("Grid not found"));

        GridEntity gridRef = entityManager.getReference(GridEntity.class, gridId);

        PlaceEntity place = placeRepo
                .findByPlaceId(details.getPlace_id())
                .orElseGet(()->{
                    PlaceEntity entity = PlaceEntity.fromDetailsDto(details);
                    entity.setGrid(gridRef);
                    return entity;
                });

        if(place.getId() !=null){
            place.updateFromDetailsDto(details);
        }
        return placeRepo.save(place);
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
