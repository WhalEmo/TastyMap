package com.beem.TastyMap.maps.geo;

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
