package com.beem.TastyMap.rag.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationReq {
    private String query;
    private boolean ignoreAllergies = false;
    private Boolean isOpenNow;
}