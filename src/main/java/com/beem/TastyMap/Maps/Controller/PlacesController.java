package com.beem.TastyMap.Maps.Controller;

import com.beem.TastyMap.Maps.Data.PlaceDetailsResponse;
import com.beem.TastyMap.Maps.Data.PlacesResponse;
import com.beem.TastyMap.Maps.Data.ScanRequest;
import com.beem.TastyMap.Maps.Service.PlacesService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/places")
public class PlacesController {

    private final PlacesService placesService;

    public PlacesController(PlacesService placesService) {
        this.placesService = placesService;
    }

    @GetMapping("/nearby")
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
