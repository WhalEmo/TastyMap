package com.beem.TastyMap.maps.data.geojson;

import java.util.HashMap;
import java.util.Map;

public class Feature {
    private final String type = "Feature";
    private Geometry geometry;
    private Map<String, Object> properties = new HashMap<>();

    public String getType() { return type; }
    public Geometry getGeometry() { return geometry; }
    public void setGeometry(Geometry geometry) { this.geometry = geometry; }
    public Map<String, Object> getProperties() { return properties; }
    public void addProperty(String key, Object value) { this.properties.put(key, value); }
}
