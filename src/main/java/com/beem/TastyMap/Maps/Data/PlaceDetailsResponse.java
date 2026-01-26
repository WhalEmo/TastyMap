package com.beem.TastyMap.Maps.Data;


public class PlaceDetailsResponse {
    private String status;
    private PlaceDetailsResult result;

    public PlaceDetailsResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PlaceDetailsResult getResult() {
        return result;
    }

    public void setResult(PlaceDetailsResult result) {
        this.result = result;
    }
}
