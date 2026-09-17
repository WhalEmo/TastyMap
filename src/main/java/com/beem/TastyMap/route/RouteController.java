package com.beem.TastyMap.route;

import com.beem.TastyMap.BaseApiResponse;
import com.beem.TastyMap.route.dto.RouteDirectionResponse;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/{id}/directions")
    public BaseApiResponse<RouteDirectionResponse> getDirections(
            @PathVariable String id,
            @RequestParam Double userLat,
            @RequestParam Double userLng,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        RouteDirectionResponse directions = routeService.calculateRoute(
                id,
                userLat,
                userLng,
                forceRefresh
        );

        return BaseApiResponse.success(
                directions
        );
    }
}
