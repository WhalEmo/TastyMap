package com.beem.TastyMap.UserRelated.Health.DTOs;

import com.beem.TastyMap.UserRelated.Health.HealthEnum;

import java.util.List;

public class HealthRequestDTO {
    private boolean hasDiabetes;
    private HealthEnum eatType;
    private List<Long> allergyIds;

    public boolean isHasDiabetes() {
        return hasDiabetes;
    }

    public void setHasDiabetes(boolean hasDiabetes) {
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
