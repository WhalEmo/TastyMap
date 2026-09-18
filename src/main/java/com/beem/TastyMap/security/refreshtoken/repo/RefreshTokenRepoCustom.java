package com.beem.TastyMap.security.refreshtoken.repo;

import com.beem.TastyMap.user.profile.dto.ActiveDeviceDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefreshTokenRepoCustom {
    List<ActiveDeviceDTO> findActiveDevices(@Param("userId") Long userId);
}
