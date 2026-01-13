package com.beem.TastyMap.Maps.Service;

import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Repository.PlaceRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class GooglePlacesService {

    private final WebClient webClient;
    private final PlaceRepo placeRepo;

    @Value("${google.maps.api.key}")
    private String apiKey;

    public GooglePlacesService(WebClient webClient, PlaceRepo placeRepo) {
        this.webClient = webClient;
        this.placeRepo = placeRepo;
    }

    public PlacesResponse getNearbyFoodPlaces(ScanRequest requestDto) {


        PlacesResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/nearbysearch/json")
                        .queryParam("location", requestDto.getLat() + "," + requestDto.getLng())
                        .queryParam("radius", requestDto.getRadius())
                        .queryParam("keyword", requestDto.joiningFormat())
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(PlacesResponse.class)
                .block();

        List<PlaceEntity> places = response.getResults()
                .stream()
                .map(PlaceEntity::fromDto).toList();

        placeRepo.saveAll(places);

        return response;
    }
}
