package com.beem.TastyMap.Maps.Service;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.Maps.Data.PlaceDetailsResponse;
import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Geo.GridCell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;


@Service
public class GooglePlacesService {

    private final WebClient webClient;
    private final RedisCacheService redisCacheService;

    @Value("${google.maps.api.key}")
    private String apiKey;

    public GooglePlacesService(WebClient webClient, RedisCacheService redisCacheService) {
        this.webClient = webClient;
        this.redisCacheService = redisCacheService;
    }

    public PlacesResponse getNearbyFoodPlaces(GridCell gridCell, String key) {
        String originalKey = key + ":getNearbyFoodPlaces";

        if(redisCacheService.exists(originalKey)){
            throw new CustomExceptions.RedisKeyExistsException("API not calling");
        }

        PlacesResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/nearbysearch/json")
                        .queryParam("location", gridCell.getLat() + "," + gridCell.getLng())
                        .queryParam("radius", 354)
                        .queryParam("keyword", gridCell.joiningFormat())
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(PlacesResponse.class)
                .block();

        redisCacheService.set(originalKey, null, 180);

        return response;
    }

    public PlaceDetailsResponse getDetailPlaceInfo(String placeId, String key){
        String originalKey = key + ":getDetailPlaceInfo";
        if(redisCacheService.exists(originalKey)){
            throw new CustomExceptions.RedisKeyExistsException("API not calling");
        }

        redisCacheService.set(originalKey, null, 180);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam(
                                "fields",
                                String.join(",",
                                        "place_id",
                                        "name",
                                        "rating",
                                        "user_ratings_total",
                                        "price_level",
                                        "types",
                                        "formatted_phone_number",
                                        "international_phone_number",
                                        "website",
                                        "opening_hours",
                                        "geometry",
                                        "photos",
                                        "reviews",
                                        "formatted_address"
                                )
                        )
                        .queryParam("language", "tr")
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(PlaceDetailsResponse.class)
                .block();
    }
}
