package com.beem.TastyMap.rag.tool;

import com.beem.TastyMap.rag.component.PlaceMetadataExtractor;
import com.beem.TastyMap.rag.data.dto.PresentedPlaceSummary;
import com.beem.TastyMap.rag.data.dto.SearchResult;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.service.RestaurantSearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Component("searchRestaurantsTool")
public class RestaurantSearchTool implements Function<RestaurantSearchTool.Request, RestaurantSearchTool.Response> {

    private static final Logger log = LoggerFactory.getLogger(RestaurantSearchTool.class);

    private final RestaurantSearchService searchService;
    private final PlaceMetadataExtractor metadataExtractor;

    public RestaurantSearchTool(RestaurantSearchService searchService, PlaceMetadataExtractor metadataExtractor) {
        this.searchService = searchService;
        this.metadataExtractor = metadataExtractor;
    }

    @JsonClassDescription("Restoran, kafe ve mekan arama parametreleri")
    public record Request(
            @JsonProperty(required = true, value = "searchQuery")
            @JsonPropertyDescription("Aranacak mekan türü veya yemek kriteri (Örn: 'sakin kafe', 'kebap')")
            String searchQuery
    ) {}

    public record Response(
            List<PresentedPlaceSummary> places,
            int remainingCount,
            String status
    ) {}

    @Override
    public Response apply(Request request) {
        log.info("🤖 MODEL TOOL KULLANDI -> Query: '{}'", request.searchQuery());

        Set<String> excludedIds = ToolSessionContext.getAlreadyPresentedIds();
        Double lat = ToolSessionContext.getUserLat();
        Double lon = ToolSessionContext.getUserLon();

        UserQueryEvent searchEvent = new UserQueryEvent(
                ToolSessionContext.getSessionId(),
                ToolSessionContext.getUserId(),
                request.searchQuery(),
                false,
                null,
                lat,
                lon,
                10.0
        );

        SearchResult result = searchService.findFreshPlaces(searchEvent, excludedIds);

        if (result.documents().isEmpty()) {
            return new Response(Collections.emptyList(), 0, "Yeni mekan bulunamadı.");
        }

        List<PresentedPlaceSummary> summaries = result.documents().stream()
                .map(metadataExtractor::fromDocument)
                .toList();

        ToolSessionContext.registerNewlyFoundPlaces(summaries);

        return new Response(summaries, result.remainingCount(), "Mekanlar bulundu.");
    }
}