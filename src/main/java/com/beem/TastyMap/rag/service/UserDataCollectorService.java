package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.userRelated.health.entitys.UserAllergiesEntity;
import com.beem.TastyMap.userRelated.health.entitys.UserHealthEntity;
import com.beem.TastyMap.userRelated.health.repos.UserAllergiesRepo;
import com.beem.TastyMap.userRelated.health.repos.UserHealthRepo;
import com.beem.TastyMap.userRelated.visit.VisitResponseDTO;
import com.beem.TastyMap.userRelated.visit.VisitService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDataCollectorService {

    private final UserAllergiesRepo userAllergiesRepo;
    private final UserHealthRepo userHealthRepo;
    private final VisitService visitService;

    @Data
    @Builder
    public static class UserContextData {
        private Optional<UserHealthEntity> healthInfo;
        private List<UserAllergiesEntity> allergies;
        private List<VisitResponseDTO> recentVisits;
    }

    public UserContextData collectUserData(Long userId) {
        Page<VisitResponseDTO> visitPage = visitService.getVisits(userId, 0, 10);
        List<VisitResponseDTO> recentVisits = visitPage != null ? visitPage.getContent() : List.of();

        return UserContextData.builder()
                .healthInfo(userHealthRepo.findByUserId(userId))
                .allergies(userAllergiesRepo.findByUserId(userId))
                .recentVisits(recentVisits)
                .build();
    }
}