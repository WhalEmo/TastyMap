package com.beem.TastyMap.rag.tool;

import com.beem.TastyMap.rag.data.dto.PresentedPlaceSummary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ToolSessionContext {

    private static final ThreadLocal<ContextData> CONTEXT = new ThreadLocal<>();

    private record ContextData(
            String sessionId,
            Long userId,
            Double lat,
            Double lon,
            Set<String> alreadyPresentedIds,
            List<PresentedPlaceSummary> newlyFoundPlaces
    ) {}

    public static void init(String sessionId, Long userId, Double lat, Double lon, Set<String> presentedIds) {
        CONTEXT.set(new ContextData(sessionId, userId, lat, lon, new HashSet<>(presentedIds), new ArrayList<>()));
    }

    public static String getSessionId() { return CONTEXT.get().sessionId(); }
    public static Long getUserId() { return CONTEXT.get().userId(); }
    public static Double getUserLat() { return CONTEXT.get().lat(); }
    public static Double getUserLon() { return CONTEXT.get().lon(); }
    public static Set<String> getAlreadyPresentedIds() { return CONTEXT.get().alreadyPresentedIds(); }

    public static void registerNewlyFoundPlaces(List<PresentedPlaceSummary> places) {
        CONTEXT.get().newlyFoundPlaces().addAll(places);
    }

    public static List<PresentedPlaceSummary> getNewlyFoundPlaces() {
        return CONTEXT.get() != null ? CONTEXT.get().newlyFoundPlaces() : List.of();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}