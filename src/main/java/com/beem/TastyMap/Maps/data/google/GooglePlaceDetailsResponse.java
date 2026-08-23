package com.beem.TastyMap.maps.data.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaceDetailsResponse {
    private String status;
    private GooglePlaceDetailsDto result;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public GooglePlaceDetailsDto getResult() { return result; }
    public void setResult(GooglePlaceDetailsDto result) { this.result = result; }
}