package com.beem.TastyMap.Maps;

public class GeoUtils {
    private static final double GRID_SIZE_DEG = 0.0045;//500 metre

    public static double roundToGrid(double coordinate) {
        return Math.floor(coordinate / GRID_SIZE_DEG) * GRID_SIZE_DEG;
    }
}
