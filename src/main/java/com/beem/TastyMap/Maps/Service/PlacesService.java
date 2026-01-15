package com.beem.TastyMap.Maps.Service;


import com.beem.TastyMap.Maps.Data.PlaceResult;
import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.GeoUtils;
import com.beem.TastyMap.Maps.GeoUtils.GridCell;
import com.beem.TastyMap.Maps.Redis.RedisKeyGenerator;
import com.beem.TastyMap.Maps.Repository.PlaceRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class PlacesService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GooglePlacesService googlePlacesService;
    private final PlaceRepo placeRepo;

    public PlacesService(RedisTemplate<String, String> redisTemplate, GooglePlacesService googlePlacesService, PlaceRepo placeRepo) {
        this.redisTemplate = redisTemplate;
        this.googlePlacesService = googlePlacesService;
        this.placeRepo = placeRepo;
    }

    public PlacesResponse getNearbyFoodPlaces(ScanRequest requestDto, GridCell cell) throws JsonProcessingException {

        BigDecimal gridLat = cell.lat();        //GeoUtils.roundToGrid(requestDto.getLat());
        BigDecimal gridLng = cell.lng();        //GeoUtils.roundToGrid(requestDto.getLng());


        String key = RedisKeyGenerator.createNearbyKey(
                gridLat,
                gridLng,
                requestDto.getRadius(),
                requestDto.getKeywords());

        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return mapper.readValue(cached, new TypeReference<PlacesResponse>() {});
        }

        List<PlaceEntity> places = placeRepo.findByGridAndTypes(gridLat, gridLng, requestDto.getKeywords());

        if(!places.isEmpty()){
            PlacesResponse response = new PlacesResponse();
            List<PlaceResult> placeResults = places.stream()
                    .map(PlaceResult::fromEntity)
                    .toList();
            response.setResults(placeResults);
            response.setStatus(cell.lat().doubleValue() + ", " + cell.lng().doubleValue());
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(response), Duration.ofHours(1));
            return response;
        }

        System.out.println("SEND API REQUEST");
        PlacesResponse response = googlePlacesService.getNearbyFoodPlaces(requestDto);

        places = response.getResults()
                .stream()
                .map(placeResult -> {
                    PlaceEntity entity = PlaceEntity.fromDto(placeResult);
                    entity.setGridLat(gridLat);
                    entity.setGridLng(gridLng);
                    return entity;
                })
                .toList();

        if(!places.isEmpty()){
            for(PlaceEntity entity: places){
                try {
                    placeRepo.save(entity);
                }catch (DataIntegrityViolationException ignored){
                }
            }
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(response), Duration.ofHours(1));
            return response;
        }

        return response;
    }

    public PlacesResponse getPlaces(ScanRequest dto){
        List<GridCell> gridCells = GeoUtils.gridCells(
                dto.getLat(),
                dto.getLng(),
                dto.getRadius()
        );

        if (gridCells.isEmpty()) {
            return emptyResponse("NO_GRID");
        }

        List<PlacesResponse> responses = gridCells.stream()
                .map(cell -> fetchPlacesSafely(dto, cell))
                .toList();

        List<PlaceResult> allResults = responses.stream()
                .flatMap(r -> r.getResults().stream())
                .toList();

        String status = resolveStatus(responses);

        PlacesResponse response = new PlacesResponse();
        response.setStatus(status);
        response.setResults(allResults);

        return response;
    }

    private PlacesResponse fetchPlacesSafely(ScanRequest dto, GridCell cell) {
        try {
            return getNearbyFoodPlaces(dto, cell);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Places API parse error for cell: " + cell, e
            );
        }
    }
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
