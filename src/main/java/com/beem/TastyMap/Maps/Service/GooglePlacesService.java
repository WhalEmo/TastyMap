package com.beem.TastyMap.Maps.Service;

import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
public class GooglePlacesService {

    private final WebClient webClient;

    @Value("${google.maps.api.key}")
    private String apiKey;

    public GooglePlacesService(WebClient webClient) {
        this.webClient = webClient;
    }

    public PlacesResponse getNearbyFoodPlaces(ScanRequest requestDto) {


        PlacesResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/nearbysearch/json")
                        .queryParam("location", requestDto.getLat() + "," + requestDto.getLng())
                        .queryParam("radius", 354)
                        .queryParam("keyword", requestDto.joiningFormat())
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(PlacesResponse.class)
                .block();

        return response;
    }
}
