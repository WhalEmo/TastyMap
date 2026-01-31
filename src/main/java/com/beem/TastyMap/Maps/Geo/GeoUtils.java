package com.beem.TastyMap.Maps.Geo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class GeoUtils {
    private static final double GRID_SIZE_DEG = 0.0045;//500 metre

    public static BigDecimal roundToGrid(double coordinate) {
        BigDecimal grid = BigDecimal.valueOf(coordinate)
                .divide(GRID_SIZE_DEG_BD, 0, RoundingMode.FLOOR)
                .multiply(GRID_SIZE_DEG_BD);

        return normalizeGrid(grid);
    }

    public static final BigDecimal GRID_SIZE_DEG_BD =
            BigDecimal.valueOf(GRID_SIZE_DEG);

    public static BigDecimal normalizeGrid(BigDecimal value) {
        return value.setScale(4, RoundingMode.FLOOR);
    }

    private static final BigDecimal HALF_GRID =
            GRID_SIZE_DEG_BD.divide(BigDecimal.valueOf(2));

    public static BigDecimal roundToGridCenter(double coordinate) {
        BigDecimal value = BigDecimal.valueOf(coordinate)
                .divide(GRID_SIZE_DEG_BD, 0, RoundingMode.FLOOR)
                .multiply(GRID_SIZE_DEG_BD)
                .add(HALF_GRID)
                .setScale(6, RoundingMode.HALF_UP);
        return normalizeGrid(value);
    }

    public static List<GridCell> gridCells(double lat, double lng, int radiusMeters, List<String> types) {

        int gridRange = (int) Math.ceil(radiusMeters / 500.0);

        BigDecimal baseCenterLat = roundToGridCenter(lat);
        BigDecimal baseCenterLng = roundToGridCenter(lng);

        int gridSize = gridRange * 2 + 1;
        int offset = -gridRange;

        List<GridCell> gridCenters = new ArrayList<>();

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {

                BigDecimal centerLat =
                        baseCenterLat.add(GRID_SIZE_DEG_BD.multiply(BigDecimal.valueOf(i + offset)));

                BigDecimal centerLng =
                        baseCenterLng.add(GRID_SIZE_DEG_BD.multiply(BigDecimal.valueOf(j + offset)));

                gridCenters.add(new GridCell(
                        normalizeGrid(centerLat),
                        normalizeGrid(centerLng),
                        types
                ));
            }
        }

        return gridCenters;
    }

    public static double centerDistance(
            BigDecimal centerLat,
            BigDecimal centerLng,
            BigDecimal nearbyLat,
            BigDecimal nearbyLng
    ) {
        final double EARTH_RADIUS = 6371000.0; // metre

        double lat1 = Math.toRadians(centerLat.doubleValue());
        double lon1 = Math.toRadians(centerLng.doubleValue());
        double lat2 = Math.toRadians(nearbyLat.doubleValue());
        double lon2 = Math.toRadians(nearbyLng.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceMeters = EARTH_RADIUS * c;

        return distanceMeters;
    }


    public static void printCells(List<GridCell> gridCells){
        int sqrt = (int) Math.sqrt(gridCells.size());
        for(int i=0; i<sqrt; i++){
            for (int j=0; j<sqrt; j++){
                printCell(gridCells.get(i*sqrt + j));
                System.out.print("\t");
            }
            System.out.print("\n");
        }
    }

    private static void printCell(GridCell cell){
        System.out.print(
                String.format(
                        "[%.5f -   %.5f]",
                        cell.getLat().doubleValue(),
                        cell.getLng().doubleValue()
                )
        );
    }

}
