package com.beem.TastyMap.rag.data.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PlaceSummaryExtraction(
        @JsonProperty("highlighted_foods") List<String> highlightedFoods,
        @JsonProperty("atmosphere") List<String> atmosphere,
        @JsonProperty("service_notes") List<String> serviceNotes,
        @JsonProperty("cautions") List<String> cautions
) {}