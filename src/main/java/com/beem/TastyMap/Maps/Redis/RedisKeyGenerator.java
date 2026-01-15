package com.beem.TastyMap.Maps.Redis;

import com.beem.TastyMap.Maps.GeoUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedisKeyGenerator {

    public static String createNearbyKey(
            BigDecimal lat,
            BigDecimal lng,
            int radius,
            List<String> types
    ) {
        List<String> sortedTypes = new ArrayList<>(types);
        Collections.sort(sortedTypes);

        String typeStr = String.join("_", sortedTypes);

        String key = String.format(
                "nearby:g%.4f:%.4f:r%d:t:%s",
                lat.doubleValue(),
                lng.doubleValue(),
                radius,
                typeStr
        );
        return key;
    }

}
