package com.beem.TastyMap.maps.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class OpeningHours {
    private Boolean open_now;
    private List<String> weekday_text;

    public OpeningHours() {
    }

    public OpeningHours(Boolean open_now, List<String> weekday_text) {
        this.open_now = open_now;
        this.weekday_text = weekday_text;
    }

    public List<String> getWeekday_text() {
        return weekday_text;
    }

    public void setWeekday_text(List<String> weekday_text) {
        this.weekday_text = weekday_text;
    }

    public Boolean getOpen_now() {
        return open_now;
    }

    public void setOpen_now(Boolean open_now) {
        this.open_now = open_now;
    }
}
