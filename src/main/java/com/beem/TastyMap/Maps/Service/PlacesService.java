package com.beem.TastyMap.Maps.Service;


import com.beem.TastyMap.Maps.Data.PlaceResult;
import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Entity.GridEntity;
import com.beem.TastyMap.Maps.Entity.GridStatus;
import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Geo.GeoUtils;
import com.beem.TastyMap.Maps.Geo.GridCell;
import com.beem.TastyMap.Maps.Redis.RedisKeyGenerator;
import com.beem.TastyMap.Maps.Repository.GridRepo;
import com.beem.TastyMap.Maps.Repository.PlaceRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PlacesService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GooglePlacesService googlePlacesService;
    private final PlaceRepo placeRepo;
    private final GridRepo gridRepo;

    public PlacesService(RedisTemplate<String, String> redisTemplate, GooglePlacesService googlePlacesService, PlaceRepo placeRepo, GridRepo gridRepo) {
        this.redisTemplate = redisTemplate;
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
        String key = cell.getGridKey();

        List<PlaceResult> cacheData = null;

        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                cacheData = mapper.readValue(cached, new TypeReference<List<PlaceResult>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return cacheData;
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

        try {
            redisTemplate.opsForValue()
                    .set(cell.getGridKey(), mapper.writeValueAsString(placeResults), Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

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
        try {
            redisTemplate.opsForValue().set(
                    cell.getGridKey(),
                    mapper.writeValueAsString(results),
                    Duration.ofHours(1)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis cache failed for grid {}");
        }
    }

    private void markAPICounter(GridCell cell){
        try {
            redisTemplate.opsForValue().set(
                    cell.getGridKey() + ":API",
                    mapper.writeValueAsString(null),
                    Duration.ofHours(1)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis cache failed for grid {}");
        }
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
