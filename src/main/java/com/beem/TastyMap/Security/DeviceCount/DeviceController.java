package com.beem.TastyMap.Security.DeviceCount;


import com.beem.TastyMap.Security.RefreshTokenRequestDTO;
import com.beem.TastyMap.Security.RefreshTokenService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final RefreshTokenService refreshTokenService;

    public DeviceController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/active")
    public Map<String, Object>getActiveDevices(Authentication authentication){
        Long userId=(Long)authentication.getPrincipal();
        List<ActiveDeviceDTO> devices=refreshTokenService.getActiveDevices(userId);
        Long count=refreshTokenService.getActiveDeviceCount(userId);
        Map<String,Object> response=new HashMap<>();
        response.put("activeDeviceCount", count);
        response.put("devices", devices);
        return response;
    }
    @PostMapping("/logout")
    public void logout(@RequestBody RefreshTokenRequestDTO dto,Authentication authentication) {
        Long userId=(Long)authentication.getPrincipal();
        refreshTokenService.logout(dto,userId);
    }
}
