package com.beem.TastyMap.Maps.Service;


import com.beem.TastyMap.Maps.Data.PlaceResult;
import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Entity.GridEntity;
import com.beem.TastyMap.Maps.Entity.GridStatus;
import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Geo.GeoUtils;
import com.beem.TastyMap.Maps.Geo.GridCell;
import com.beem.TastyMap.Maps.Repository.GridRepo;
import com.beem.TastyMap.Maps.Repository.PlaceRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PlacesService {

    private final RedisCacheService redisService;
    private final GooglePlacesService googlePlacesService;
    private final PlaceRepo placeRepo;
    private final GridRepo gridRepo;

    public PlacesService(RedisCacheService service, GooglePlacesService googlePlacesService, PlaceRepo placeRepo, GridRepo gridRepo) {
        this.redisService = service;
        this.googlePlacesService = googlePlacesService;
        this.placeRepo = placeRepo;
        this.gridRepo = gridRepo;
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

            returnData = searchCache(cell);

            if(returnData != null){
                results.addAll(returnData);
                continue;
            }

            returnData = searchDataBase(cell);

            if(returnData != null){
                results.addAll(returnData);
                continue;
            }

            returnData = searchGoogleAPI(cell);

            results.addAll(returnData);


        }

        PlacesResponse response = new PlacesResponse();
        response.setResults(results);
        response.setStatus("Ok");

        return response;
    }

    private List<PlaceResult> searchCache(GridCell cell){
        return redisService.getWithSlidingTTL(
                cell.getGridKey(),
                new TypeReference<List<PlaceResult>>() {},
                3600
        );
    }

    private List<PlaceResult> searchDataBase(GridCell cell){
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

        cacheResults(cell, placeResults);

        return placeResults;
    }

    @Transactional
    private List<PlaceResult> searchGoogleAPI(GridCell cell){

        List<PlaceResult> placeResults = fetchAndFilterPlaces(cell);

        markAPICounter(cell);

        GridEntity grid = getOrCreatedGrid(cell);

        if(placeResults.isEmpty()){
            markGridAsEmpty(grid);
            cacheResults(cell, placeResults);
            return Collections.emptyList();
        }

        markGridAsHasData(grid);

        persistPlaces(grid, placeResults);

        cacheResults(cell, placeResults);

        return placeResults;
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


    private void cacheResults(GridCell cell, List<PlaceResult> results) {
        redisService.set(
                cell.getGridKey(),
                results,
                3600
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
    
}
