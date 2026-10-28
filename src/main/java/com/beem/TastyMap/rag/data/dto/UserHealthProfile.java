package com.beem.TastyMap.rag.data.dto;

import com.beem.TastyMap.user.health.model.HealthEnum;

import java.util.Collections;
import java.util.Set;

public record UserHealthProfile(
        HealthEnum eatType,
        boolean hasDiabetes,
        Set<String> allergies
) {
    public static UserHealthProfile empty() {
        return new UserHealthProfile(HealthEnum.NORMAL, false, Collections.emptySet());
    }

    public boolean isRestricted() {
        return eatType != HealthEnum.NORMAL || hasDiabetes || (allergies != null && !allergies.isEmpty());
    }

    public boolean isVegan() {
        return eatType == HealthEnum.VEGAN;
    }

    public boolean isVegetarian() {
        return eatType == HealthEnum.VEGETARIAN || eatType == HealthEnum.VEGAN;
    }

    public boolean isCeliac() {
        if (allergies == null || allergies.isEmpty()) {
            return false;
        }
        return allergies.stream()
                .map(String::toLowerCase)
                .anyMatch(a -> a.contains("gluten") || a.contains("çölyak") || a.contains("colyak") || a.contains("celiac"));
    }

    @Override
    public String toString() {
        String allergyStr = (allergies == null || allergies.isEmpty()) ? "Yok" : String.join(", ", allergies);
        return String.format("[Beslenme: %s | Diyabet: %s | Alerjiler: %s]",
                eatType != null ? eatType : HealthEnum.NORMAL,
                hasDiabetes ? "Var" : "Yok",
                allergyStr);
    }
}