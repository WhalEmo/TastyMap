package com.beem.TastyMap.Maps.Data;

import java.util.List;

public class ScanRequest {
    private Double lat;
    private Double lng;
    private int radius;
    private List<String> keywords;

    public ScanRequest() {
    }

    public String joiningFormat() {
        return String.join("|", keywords);
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keyword) {
        this.keywords = keyword;
    }
}
