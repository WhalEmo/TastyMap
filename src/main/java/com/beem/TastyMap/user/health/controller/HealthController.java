package com.beem.TastyMap.user.health.controller;


import com.beem.TastyMap.user.health.dto.HealthRequestDTO;
import com.beem.TastyMap.user.health.dto.HealthResponseDTO;
import com.beem.TastyMap.user.health.service.HealthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/HealthInfo")
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @PostMapping("/addHealth")
    public HealthResponseDTO addHealth(
            @Valid @RequestBody HealthRequestDTO dto,
            Authentication authentication
    ){
        Long myId=(Long)authentication.getPrincipal();
        return healthService.addHealthInfo(dto,myId);
    }

    @PatchMapping("/updateHealth")
    public HealthResponseDTO updateHealth(
            @Valid @RequestBody HealthRequestDTO dto,
            Authentication authentication
    ){
        Long myId=(Long)authentication.getPrincipal();
        return healthService.updateHealth(dto,myId);
    }

    @GetMapping("/getHealthInfo")
    public HealthResponseDTO getHealth(
            Authentication authentication
    ){
        Long myId=(Long)authentication.getPrincipal();
        return healthService.getHealthInfo(myId);
    }

}
