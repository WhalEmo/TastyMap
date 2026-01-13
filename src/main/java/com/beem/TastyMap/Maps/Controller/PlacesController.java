package com.beem.TastyMap.Maps.Controller;

import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Service.GooglePlacesService;
import com.beem.TastyMap.Maps.Service.PlacesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/places")
public class PlacesController {

    private final PlacesService placesService;

    public PlacesController(PlacesService placesService) {
        this.placesService = placesService;
    }

    @GetMapping("/nearby")
    public PlacesResponse nearbyPlaces(@RequestBody ScanRequest requestDto) throws JsonProcessingException {
        return placesService.getNearbyFoodPlaces(requestDto);
    }
}
