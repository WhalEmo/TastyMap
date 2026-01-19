package com.beem.TastyMap.Maps.Redis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedisKeyGenerator {

    public static String createNearbyKey(
            BigDecimal lat,
            BigDecimal lng,
            List<String> types
    ) {
        List<String> sortedTypes = new ArrayList<>(types);
        Collections.sort(sortedTypes);

        String typeStr = String.join("_", sortedTypes);

        String key = String.format(
                "grid:g%.4f:%.4f:t:%s",
                lat.doubleValue(),
                lng.doubleValue(),
                typeStr
        );
        return key;
    }

}
