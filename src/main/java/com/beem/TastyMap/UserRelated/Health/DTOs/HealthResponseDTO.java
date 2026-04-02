package com.beem.TastyMap.UserRelated.Health.DTOs;

import java.util.List;

public class HealthResponseDTO {
    private boolean hasDiabetes;
    private String eatType;
    private List<AllergyInfoDTO> allergyInfo;


    public HealthResponseDTO(boolean hasDiabetes, String eatType, List<AllergyInfoDTO> allergyInfo) {
        this.hasDiabetes = hasDiabetes;
        this.eatType = eatType;
        this.allergyInfo = allergyInfo;
    }

    public boolean isHasDiabetes() {
        return hasDiabetes;
    }

    public void setHasDiabetes(boolean hasDiabetes) {
        this.hasDiabetes = hasDiabetes;
    }

    public String getEatType() {
        return eatType;
    }

    public void setEatType(String eatType) {
        this.eatType = eatType;
    }

    public List<AllergyInfoDTO> getAllergyInfo() {
        return allergyInfo;
    }

    public void setAllergyInfo(List<AllergyInfoDTO> allergyInfo) {
        this.allergyInfo = allergyInfo;
    }
}
