package com.beem.TastyMap.userRelated.health;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.health.dtos.AllergyInfoDTO;
import com.beem.TastyMap.userRelated.health.dtos.HealthRequestDTO;
import com.beem.TastyMap.userRelated.health.dtos.HealthResponseDTO;
import com.beem.TastyMap.userRelated.health.entitys.AllergiesEntity;
import com.beem.TastyMap.userRelated.health.entitys.UserAllergiesEntity;
import com.beem.TastyMap.userRelated.health.entitys.UserHealthEntity;
import com.beem.TastyMap.userRelated.health.repos.AllergiesRepo;
import com.beem.TastyMap.userRelated.health.repos.UserAllergiesRepo;
import com.beem.TastyMap.userRelated.health.repos.UserHealthRepo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HealthService {
    private final UserHealthRepo userHealthRepo;
    private final AllergiesRepo allergiesRepo;
    private final UserAllergiesRepo userAllergiesRepo;
    private final EntityManager entityManager;

    public HealthService(UserHealthRepo userHealthRepo, AllergiesRepo allergiesRepo, UserAllergiesRepo userAllergiesRepo, EntityManager entityManager) {
        this.userHealthRepo = userHealthRepo;
        this.allergiesRepo = allergiesRepo;
        this.userAllergiesRepo = userAllergiesRepo;
        this.entityManager = entityManager;
    }

    @Transactional
    public HealthResponseDTO addHealthInfo(HealthRequestDTO dto, Long userId) {
        UserEntity userRef = entityManager.getReference(UserEntity.class, userId);

        UserHealthEntity userHealthEntity = new UserHealthEntity();
        userHealthEntity.setUser(userRef);
        userHealthEntity.setEatType(dto.getEatType());
        userHealthEntity.setHasDiabetes(dto.isHasDiabetes());
        userHealthRepo.save(userHealthEntity);
        List<UserAllergiesEntity> mappingsToSave = new ArrayList<>();
        List<AllergyInfoDTO> responseAllergyList = new ArrayList<>();

        for (Long allergyId : dto.getAllergyIds()) {
            AllergiesEntity allergy = allergiesRepo.findById(allergyId)
                    .orElseThrow(() -> new RuntimeException("Alerji bulunamadı: " + allergyId));

            UserAllergiesEntity mapping = new UserAllergiesEntity();
            mapping.setUser(userRef);
            mapping.setAllergies(allergy);
            mappingsToSave.add(mapping);
            responseAllergyList.add(new AllergyInfoDTO(allergy.getId(), allergy.getAllergyName()));
        }
        if (!mappingsToSave.isEmpty()) {
            userAllergiesRepo.saveAll(mappingsToSave);
        }
        return new HealthResponseDTO(
                userHealthEntity.isHasDiabetes(),
                userHealthEntity.getEatType().name(),
                responseAllergyList
        );
    }

    @Transactional
    public HealthResponseDTO updateHealth(HealthRequestDTO dto, Long userId) {
        UserHealthEntity healthData = userHealthRepo.findByUserId(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Sağlık profili bulunamadı."));

        healthData.setEatType(dto.getEatType());
        healthData.setHasDiabetes(dto.isHasDiabetes());

        List<UserAllergiesEntity> currentMappings = userAllergiesRepo.findAllByUserIdWithAllergies(userId);

        Set<Long> existingAllergyIds = currentMappings.stream()
                .map(m -> m.getAllergies().getId())
                .collect(Collectors.toSet());

        Set<Long> incomingAllergyIds = dto.getAllergyIds() != null ? new HashSet<>(dto.getAllergyIds()) : new HashSet<>();

        List<UserAllergiesEntity> mappingsToDelete = currentMappings.stream()
                .filter(m -> !incomingAllergyIds.contains(m.getAllergies().getId()))
                .toList();

        if (!mappingsToDelete.isEmpty()) {
            userAllergiesRepo.deleteAllInBatch(mappingsToDelete);
        }

        List<Long> idsToFetch = incomingAllergyIds.stream()
                .filter(id -> !existingAllergyIds.contains(id))
                .toList();

        if (!idsToFetch.isEmpty()) {
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);

            List<AllergiesEntity> allergiesToLink = allergiesRepo.findAllById(idsToFetch);

            List<UserAllergiesEntity> newMappings = allergiesToLink.stream().map(allergy -> {
                UserAllergiesEntity mapping = new UserAllergiesEntity();
                mapping.setUser(userRef);
                mapping.setAllergies(allergy);
                return mapping;
            }).toList();

            userAllergiesRepo.saveAll(newMappings);
        }

        return getHealthInfo(userId);
    }
    @Transactional(readOnly = true)
    public HealthResponseDTO getHealthInfo(Long userId) {
        UserHealthEntity healthData = userHealthRepo.findByUserId(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Sağlık profili bulunamadı."));

        List<AllergyInfoDTO> allergyInfoList = userAllergiesRepo.findAllergyInfoByUserId(userId);
        return new HealthResponseDTO(
                healthData.isHasDiabetes(),
                healthData.getEatType().name(),
                allergyInfoList
        );
    }


}
