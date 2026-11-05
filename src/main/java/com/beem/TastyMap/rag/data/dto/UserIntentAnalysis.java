package com.beem.TastyMap.rag.data.dto;

import com.beem.TastyMap.rag.data.enums.UserIntentAction;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserIntentAnalysis(
        @JsonProperty("action") UserIntentAction action,
        @JsonProperty("target_place_name") String targetPlaceName,
        @JsonProperty("semantic_query") String semanticQuery,
        @JsonProperty("direct_reply") String directReply
) {
    public static UserIntentAnalysis fallback(String query) {
        return new UserIntentAnalysis(UserIntentAction.NEW_SEARCH, "", query, "");
    }
}