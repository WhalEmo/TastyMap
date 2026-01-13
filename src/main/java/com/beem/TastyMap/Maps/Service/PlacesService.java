package com.beem.TastyMap.Maps.Service;


import com.beem.TastyMap.Maps.Data.PlaceResult;
import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Redis.RedisKeyGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
public class PlacesService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GooglePlacesService googlePlacesService;

    public PlacesService(RedisTemplate<String, String> redisTemplate, GooglePlacesService googlePlacesService) {
        this.redisTemplate = redisTemplate;
        this.googlePlacesService = googlePlacesService;
    }

    public PlacesResponse getNearbyFoodPlaces(ScanRequest requestDto) throws JsonProcessingException {

        String key = RedisKeyGenerator.createNearbyKey(
                requestDto.getLat(),
                requestDto.getLng(),
                requestDto.getRadius(),
                requestDto.getKeywords());

        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return mapper.readValue(cached, new TypeReference<PlacesResponse>() {});
        }

        PlacesResponse place = googlePlacesService.getNearbyFoodPlaces(requestDto);

        redisTemplate.opsForValue().set(key, mapper.writeValueAsString(place), Duration.ofHours(1));

        return place;
    }

    private PlaceResult getMapperPlace(){
        return null;
    }

}
