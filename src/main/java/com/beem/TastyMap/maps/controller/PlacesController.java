package com.beem.TastyMap.maps.controller;

import com.beem.TastyMap.maps.data.PlaceDetailsResponse;
import com.beem.TastyMap.maps.data.PlacesResponse;
import com.beem.TastyMap.maps.data.ScanRequest;
import com.beem.TastyMap.maps.service.PlacesService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/places")
public class PlacesController {

    private final PlacesService placesService;

    public PlacesController(PlacesService placesService) {
        this.placesService = placesService;
    }

    @PostMapping("/nearby")
    public PlacesResponse nearbyPlaces(@RequestBody ScanRequest requestDto){
        return placesService.getPlaces(requestDto);
    }

    @GetMapping("/nearby-details")
    public PlaceDetailsResponse nearbyPlaceDetails(
            @RequestParam(name = "place_id", required = true) String placeId
    ) {
        return placesService.getPlaceDetails(placeId);
    }
}
