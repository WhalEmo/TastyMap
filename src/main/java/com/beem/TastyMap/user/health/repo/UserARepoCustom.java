package com.beem.TastyMap.user.health.repo;

import com.beem.TastyMap.user.health.dto.AllergyInfoDTO;

import java.util.List;

public interface UserARepoCustom {
    List<AllergyInfoDTO> findAllergyInfoByUserId(Long userId);
}
