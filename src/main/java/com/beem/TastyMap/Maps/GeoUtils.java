package com.beem.TastyMap.Maps;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class GeoUtils {
    private static final double GRID_SIZE_DEG = 0.0045;//500 metre
    public record GridCell(BigDecimal lat, BigDecimal lng) {}

    public static BigDecimal roundToGrid(double coordinate) {
        BigDecimal grid = BigDecimal.valueOf(coordinate)
                .divide(GRID_SIZE_DEG_BD, 0, RoundingMode.FLOOR)
                .multiply(GRID_SIZE_DEG_BD);

        return normalizeGrid(grid);
    }

    private static final BigDecimal GRID_SIZE_DEG_BD =
            BigDecimal.valueOf(GRID_SIZE_DEG);

    public static BigDecimal normalizeGrid(BigDecimal value) {
        return value.setScale(4, RoundingMode.FLOOR);
    }

    public static List<GridCell> gridCells(double lat, double lng, int radiusMeters){
        int gridRange = (int) Math.ceil(radiusMeters / 500.0);
        gridRange--; // Bu daha sonra kaldırılması gerekebilir chunk düşürüldü daha az istek atılması için;
        BigDecimal baseLat = roundToGrid(lat);
        BigDecimal baseLng = roundToGrid(lng);

        int gridSize = gridRange*2 + 1;

        List<GridCell> gridCenters = new ArrayList<>();
        gridRange *= -1;
        for(int i=0; i<gridSize; i++){
            for(int j=0; j<gridSize; j++){

                gridCenters.add(new GridCell(
                        normalizeGrid(
                                baseLat.add(GRID_SIZE_DEG_BD.multiply(BigDecimal.valueOf(i+gridRange)))
                        ),
                        normalizeGrid(
                                baseLng.add(GRID_SIZE_DEG_BD.multiply(BigDecimal.valueOf(j+gridRange)))
                        )
                ));
            }
        }

        printCells(gridCenters);

        return gridCenters;
    }

    private static void printCells(List<GridCell> gridCells){
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
                        "[%.4f -   %.4f]",
                        cell.lat.doubleValue(),
                        cell.lng.doubleValue()
                )
        );
    }

}
