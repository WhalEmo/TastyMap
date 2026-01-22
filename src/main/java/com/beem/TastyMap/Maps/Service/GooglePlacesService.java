package com.beem.TastyMap.Maps.Service;

import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Geo.GridCell;
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

    public PlacesResponse getNearbyFoodPlaces(GridCell gridCell) {


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

        return response;
    }
}
