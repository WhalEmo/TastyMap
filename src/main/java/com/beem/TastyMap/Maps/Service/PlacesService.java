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

    private PlacesResponse getPlaces(ScanRequest request){
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



        }

        return null;
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

    /*
    private List<PlaceResult> getNearbyFoodPlaces(ScanRequest requestDto, GridCell cell) throws JsonProcessingException {

        BigDecimal gridLat = cell.getLat();
        BigDecimal gridLng = cell.getLng();


        String key = RedisKeyGenerator.createNearbyKey(
                gridLat,
                gridLng,
                requestDto.getRadius(),
                requestDto.getKeywords());

        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            List<PlaceResult> cacheData = mapper.readValue(cached, new TypeReference<List<PlaceResult>>() {});
            cell.setGridNearby(
                    PlaceEntity.fromDtoList(cacheData)
            );
            return cacheData;
        }

        cell.printCell();
        List<PlaceEntity> places = placeRepo.findByGridAndTypes(gridLat, gridLng, requestDto.getKeywords());
        cell.printCell();

        if(!places.isEmpty()){
            PlacesResponse response = new PlacesResponse();
            List<PlaceResult> placeResults = places.stream()
                    .map(PlaceResult::fromEntity)
                    .toList();
            response.setResults(placeResults);
            cell.setGridNearby(
                    PlaceEntity.fromDtoList(placeResults)
            );
            response.setStatus(cell.getLat().doubleValue() + ", " + cell.getLng().doubleValue());
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(response.getResults()), Duration.ofHours(1));
            return response.getResults();
        }

        return safelyRequestAndSave(requestDto,key, gridLat, gridLng);
    }

    public PlacesResponse getPlaces(ScanRequest dto){
        List<GridCell> gridCells = GeoUtils.gridCells(
                dto.getLat(),
                dto.getLng(),
                dto.getRadius()
        );

        GeoUtils.printCells(gridCells);

        if (gridCells.isEmpty()) {
            return emptyResponse("NO_GRID");
        }

        List<PlaceResult> placeResults = new ArrayList<>();
        for(GridCell cell: gridCells){
            placeResults.addAll(fetchPlacesSafely(dto,cell));
        }

        PlacesResponse response = new PlacesResponse();
        response.setResults(placeResults);
        response.setStatus("okk");

        return response;
    }

    private List<PlaceResult> safelyRequestAndSave(
            ScanRequest requestDto,
            String key,
            BigDecimal centerLat,
            BigDecimal centerLng
    ) {
        System.out.println("SEND API REQUEST");
        PlacesResponse response =
                googlePlacesService.getNearbyFoodPlaces(requestDto);

        List<PlaceResult> filteredResults = response.getResults()
                .stream()
                .filter(placeResult -> {
                    BigDecimal gridLat =
                            GeoUtils.roundToGridCenter(
                                    placeResult.getGeometry().getLocation().getLat()
                            );
                    BigDecimal gridLng =
                            GeoUtils.roundToGridCenter(
                                    placeResult.getGeometry().getLocation().getLng()
                            );

                    return gridLat.compareTo(centerLat) == 0 &&
                            gridLng.compareTo(centerLng) == 0;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        if(filteredResults.isEmpty()){
            PlaceEntity entity = new PlaceEntity();
            entity.setPlaceId("EMPTY" + centerLat + "_" + centerLng);
            entity.setStatus(GridStatus.EMPTY);
            entity.setName("EMPTY_GRID");
            filteredResults.add(PlaceResult.fromEntity(entity));
        }

        Set<String> placeIds = filteredResults
                .stream()
                .map(PlaceResult::getPlace_id)
                .collect(Collectors.toSet());

        Map<String, Long> placeIdsMapId = placeRepo.findIdsByPlaceIds(placeIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        List<PlaceEntity> places = filteredResults
                .stream()
                .map(placeResult -> {
                    PlaceEntity entity = PlaceEntity.fromDto(placeResult);

                    BigDecimal gridLat = GeoUtils.roundToGridCenter(
                            placeResult.getGeometry().getLocation().getLat() == null ? centerLat.doubleValue()
                                    : placeResult.getGeometry().getLocation().getLat()
                    );
                    gridLat = GeoUtils.normalizeGrid(gridLat);

                    BigDecimal gridLng = GeoUtils.roundToGridCenter(
                            placeResult.getGeometry().getLocation().getLng() == null ? centerLng.doubleValue()
                                    : placeResult.getGeometry().getLocation().getLng()
                    );
                    gridLng = GeoUtils.normalizeGrid(gridLng);

                    entity.setStatus(
                            entity.getStatus() == null ? GridStatus.HAS_DATA : entity.getStatus()
                    );

                    System.out.println("Database Save: " + gridLat + " : " + gridLng);

                    Long existingId = placeIdsMapId.get(entity.getPlaceId());
                    if(existingId != null) entity.setId(existingId);

                    entity.setGridLat(gridLat);
                    entity.setGridLng(gridLng);
                    return entity;
                })
                .toList();

        placeRepo.saveAll(places);

        if (!filteredResults.isEmpty()) {
            try {
                redisTemplate.opsForValue()
                        .set(key,
                                mapper.writeValueAsString(filteredResults),
                                Duration.ofHours(1));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return filteredResults;
    }

    private List<PlaceResult> fetchPlacesSafely(ScanRequest dto, GridCell cell) {
        try {
            return getNearbyFoodPlaces(dto, cell);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Places API parse error for cell: " + cell, e
            );
        }
    }
    */
    private PlacesResponse emptyResponse(String status) {
        PlacesResponse response = new PlacesResponse();
        response.setStatus(status);
        response.setResults(List.of());
        return response;
    }

    private String resolveStatus(List<PlacesResponse> responses) {
        return responses.stream()
                .map(PlacesResponse::getStatus)
                .distinct()
                .collect(Collectors.joining("/"));
    }


}
