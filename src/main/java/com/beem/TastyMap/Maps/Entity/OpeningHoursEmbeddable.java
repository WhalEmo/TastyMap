package com.beem.TastyMap.Maps.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Embeddable
public class OpeningHoursEmbeddable {

    private Boolean openNow;

    @ElementCollection
    @CollectionTable(
            name = "place_opening_weekdays",
            joinColumns = @JoinColumn(name = "place_id")
    )
    @Column(name = "weekday_text")
    private List<String> weekdayText = new ArrayList<>();

    public OpeningHoursEmbeddable() {
    }

    public OpeningHoursEmbeddable(Boolean openNow, List<String> weekdayText) {
        this.openNow = openNow;
        this.weekdayText = weekdayText;
    }

    public Boolean getOpenNow() {
        return openNow;
    }

    public void setOpenNow(Boolean openNow) {
        this.openNow = openNow;
    }

    public List<String> getWeekdayText() {
        return weekdayText;
    }

    public void setWeekdayText(List<String> weekdayText) {
        this.weekdayText = weekdayText;
    }
}
