package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.dto.UserHealthProfile;
import com.beem.TastyMap.user.health.entity.AllergiesEntity;
import com.beem.TastyMap.user.health.entity.UserAllergiesEntity;
import com.beem.TastyMap.user.health.entity.UserHealthEntity;
import com.beem.TastyMap.user.health.model.HealthEnum;
import com.beem.TastyMap.user.health.repo.UserAllergiesRepo;
import com.beem.TastyMap.user.health.repo.UserHealthRepo;
import com.beem.TastyMap.visit.dto.VisitResponseDTO;
import com.beem.TastyMap.visit.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserDataCollectorService {

    private static final Logger log = LoggerFactory.getLogger(UserDataCollectorService.class);

    private final UserAllergiesRepo userAllergiesRepo;
    private final UserHealthRepo userHealthRepo;
    private final VisitService visitService;

    public UserDataCollectorService(UserAllergiesRepo userAllergiesRepo,
                                    UserHealthRepo userHealthRepo,
                                    VisitService visitService) {
        this.userAllergiesRepo = userAllergiesRepo;
        this.userHealthRepo = userHealthRepo;
        this.visitService = visitService;
    }

    public record UserContextData(
            UserHealthProfile healthProfile,
            List<VisitResponseDTO> recentVisits
    ) {
        @Override
        public String toString() {
            int visitCount = (recentVisits != null) ? recentVisits.size() : 0;
            return String.format("UserContextData(Sağlık: %s, Son Ziyaret Sayısı: %d)",
                    healthProfile != null ? healthProfile.toString() : "Belirtilmemiş",
                    visitCount);
        }
    }

    @Transactional(readOnly = true)
    public UserContextData collectUserData(Long userId) {
        if (userId == null) {
            return new UserContextData(UserHealthProfile.empty(), List.of());
        }

        Page<VisitResponseDTO> visitPage = visitService.getVisits(userId, 0, 10);
        List<VisitResponseDTO> recentVisits = (visitPage != null && visitPage.getContent() != null)
                ? visitPage.getContent()
                : List.of();

        Optional<UserHealthEntity> optionalUserHealth = userHealthRepo.findByUserId(userId);
        HealthEnum eatType = optionalUserHealth.map(UserHealthEntity::getEatType).orElse(HealthEnum.NORMAL);
        boolean hasDiabetes = optionalUserHealth.map(UserHealthEntity::isHasDiabetes).orElse(false);

        List<UserAllergiesEntity> userAllergies = userAllergiesRepo.findAllByUserIdWithAllergies(userId);

        Set<String> allergyNames = Collections.emptySet();

        if (userAllergies != null && !userAllergies.isEmpty()) {
            allergyNames = userAllergies.stream()
                    .map(UserAllergiesEntity::getAllergies)
                    .filter(Objects::nonNull)
                    .map(AllergiesEntity::getAllergyName)
                    .filter(Objects::nonNull)
                    .map(name -> name.trim().toLowerCase(Locale.forLanguageTag("tr")))
                    .collect(Collectors.toUnmodifiableSet());
        }

        UserHealthProfile profile = new UserHealthProfile(
                eatType,
                hasDiabetes,
                allergyNames
        );

        UserContextData contextData = new UserContextData(
                profile,
                recentVisits
        );

        log.info("🩺 [UserDataCollector] Kullanıcı bağlamı toplandı (User: {}) -> {}", userId, contextData);

        return contextData;
    }
}