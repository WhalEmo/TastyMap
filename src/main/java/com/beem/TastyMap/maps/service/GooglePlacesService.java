package com.beem.TastyMap.maps.service;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.maps.data.PlacesResponse;
import com.beem.TastyMap.maps.data.google.GooglePlaceDetailsResponse;
import com.beem.TastyMap.maps.geo.GridCell;
import com.beem.TastyMap.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;


@Service
public class GooglePlacesService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);

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

        log.info("Google place api calling");

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

        if (response != null) {
            redisCacheService.set(originalKey, "SUCCESS_LOCK", 180);
        }

        return response;
    }

    public Mono<PlacesResponse> getNearbyFoodPlacesAsync(GridCell gridCell, String key) {
        String originalKey = key + ":getNearbyFoodPlaces";

        if (redisCacheService.exists(originalKey)) {
            log.warn("Bu hücre için API çağrı kilidi aktif, istek atlanıyor: {}", gridCell.getGridKey());
            PlacesResponse emptyResponse = new PlacesResponse();
            emptyResponse.setResults(Collections.emptyList());
            emptyResponse.setStatus("OK");
            return Mono.just(emptyResponse);
        }

        log.info("Google place api calling (Asenkron): {}", gridCell.getGridKey());

        return webClient.get()
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
                .retryWhen(Retry.fixedDelay(1, Duration.ofMillis(300))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal ->
                                log.warn("Soket koptu, taze bağlantıyla tekrar deneniyor (Hücre: {})", gridCell.getGridKey())
                        )
                )
                .doOnSuccess(response -> {
                    if (response != null && "OK".equals(response.getStatus())) {
                        redisCacheService.set(originalKey, "SUCCESS_LOCK", 180);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Google Places API hatası ({}): {}", gridCell.getGridKey(), e.getMessage());
                    return Mono.empty();
                });
    }

    public GooglePlaceDetailsResponse getDetailPlaceInfo(String placeId, String key){
        String originalKey = key + ":getDetailPlaceInfo";
        if(redisCacheService.exists(originalKey)){
            throw new CustomExceptions.RedisKeyExistsException("API not calling");
        }

        log.info("Google place detail api calling...");

        GooglePlaceDetailsResponse response = webClient.get()
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
                                        "reviews",
                                        "formatted_address"
                                )
                        )
                        .queryParam("language", "tr")
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(GooglePlaceDetailsResponse.class)
                .block();

        if (response != null) {
            redisCacheService.set(originalKey, "SUCCESS_LOCK", 180);
        }

        return response;
    }
}
