package com.beem.TastyMap.UserRelated.Health.Repos;

import com.beem.TastyMap.UserRelated.Health.DTOs.AllergyInfoDTO;
import com.beem.TastyMap.UserRelated.Health.DTOs.HealthResponseDTO;

import java.util.List;

public interface UserARepoCustom {
    List<AllergyInfoDTO> findAllergyInfoByUserId(Long userId);
}
