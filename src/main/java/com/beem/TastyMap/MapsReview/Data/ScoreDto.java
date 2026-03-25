package com.beem.TastyMap.MapsReview.Data;

import com.beem.TastyMap.MapsReview.Enum.ScoreType;

public class ScoreDto {

    private ScoreType type;
    private double score;

    public ScoreDto(ScoreType type, double score) {
        this.type = type;
        this.score = score;
    }

    public ScoreDto() {
    }

    public ScoreType getType() {
        return type;
    }

    public void setType(ScoreType type) {
        this.type = type;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}

