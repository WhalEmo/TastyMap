package com.beem.TastyMap.Maps.Geo;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Redis.RedisKeyGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GridCell {

    private BigDecimal lat;
    private BigDecimal lng;
    private String gridKey;
    private List<String> types;

    public GridCell() {
    }

    public GridCell(BigDecimal lat, BigDecimal lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public GridCell(BigDecimal lat, BigDecimal lng, List<String> types) {
        this.lat = lat;
        this.lng = lng;
        this.types = types;
    }

    public void printCell(){
        System.out.println(
                String.format(
                        "CELL: [%.4f -   %.4f]",
                        this.getLat().doubleValue(),
                        this.getLng().doubleValue()
                )
        );
    }

    public String joiningFormat() {
        return String.join("|", types);
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public String getGridKey() {
        return gridKey != null
                ? gridKey
                : (gridKey = RedisKeyGenerator.createNearbyKey(
                        this.lat,
                        this.lng,
                        this.types
        ));
    }
}
