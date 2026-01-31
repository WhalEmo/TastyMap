package com.beem.TastyMap.MapsReview.Data.Request;

import com.beem.TastyMap.MapsReview.Enum.ScoreType;

public class ScoreRequest {

    private ScoreType type;
    private double score;


    public ScoreRequest() {
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

