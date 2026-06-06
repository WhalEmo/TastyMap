package com.beem.TastyMap.maps.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpeningHoursEmbeddable that)) return false;

        return Objects.equals(openNow, that.openNow) &&
                Objects.equals(getWeekdayText(), that.getWeekdayText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(openNow, getWeekdayText());
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
