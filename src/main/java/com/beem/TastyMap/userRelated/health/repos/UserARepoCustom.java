package com.beem.TastyMap.userRelated.health.repos;

import com.beem.TastyMap.userRelated.health.dtos.AllergyInfoDTO;

import java.util.List;

public interface UserARepoCustom {
    List<AllergyInfoDTO> findAllergyInfoByUserId(Long userId);
}
