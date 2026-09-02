package com.beem.TastyMap.userRelated.health.dtos;

import com.beem.TastyMap.userRelated.health.HealthEnum;

import java.util.List;

public class HealthRequestDTO {
    private Boolean hasDiabetes;
    private HealthEnum eatType;
    private List<Long> allergyIds;

    public Boolean getHasDiabetes() {
        return hasDiabetes;
    }

    public void setHasDiabetes(Boolean hasDiabetes) {
        this.hasDiabetes = hasDiabetes;
    }

    public HealthEnum getEatType() {
        return eatType;
    }

    public void setEatType(HealthEnum eatType) {
        this.eatType = eatType;
    }

    public List<Long> getAllergyIds() {
        return allergyIds;
    }

    public void setAllergyIds(List<Long> allergyIds) {
        this.allergyIds = allergyIds;
    }
}
