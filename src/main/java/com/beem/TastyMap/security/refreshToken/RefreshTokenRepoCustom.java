package com.beem.TastyMap.security.refreshToken;

import com.beem.TastyMap.userRelated.profile.ActiveDeviceDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefreshTokenRepoCustom {
    List<ActiveDeviceDTO> findActiveDevices(@Param("userId") Long userId);
}
