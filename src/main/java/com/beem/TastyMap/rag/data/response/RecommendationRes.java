package com.beem.TastyMap.rag.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRes {
    private String recommendationText;
    private List<String> matchedPlaceIds;
}