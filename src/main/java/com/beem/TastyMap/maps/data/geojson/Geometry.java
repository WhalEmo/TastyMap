package com.beem.TastyMap.maps.data.geojson;

import java.util.Arrays;
import java.util.List;

public class Geometry {
    private final String type = "Point";
    private List<Double> coordinates; // [longitude, latitude]

    public Geometry(double lng, double lat) {
        this.coordinates = Arrays.asList(lng, lat);
    }

    public String getType() { return type; }
    public List<Double> getCoordinates() { return coordinates; }
}