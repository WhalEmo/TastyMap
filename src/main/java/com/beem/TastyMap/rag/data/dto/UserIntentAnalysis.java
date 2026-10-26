package com.beem.TastyMap.rag.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserIntentAnalysis(
        @JsonProperty("search_needed") boolean searchNeeded,
        @JsonProperty("is_named_entity") boolean isNamedEntity,
        @JsonProperty("semantic_query") String semanticQuery,
        @JsonProperty("direct_reply") String directReply
) {}