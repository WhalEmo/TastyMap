package com.beem.TastyMap.Maps.Redis;

import com.beem.TastyMap.Maps.GeoUtils;

import java.util.List;

public class RedisKeyGenerator {

    public static String createNearbyKey(double lat, double lng, int radius, List<String> types) {
        double gridLat = GeoUtils.roundToGrid(lat);
        double gridLng = GeoUtils.roundToGrid(lng);
        String typeStr = String.join("|", types);

        return "nearby:" + gridLat + ":" + gridLng + ":" + radius + ":" + typeStr;
    }
}
