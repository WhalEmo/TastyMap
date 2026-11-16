package com.beem.TastyMap.ai;

import java.io.Serial;
import java.io.Serializable;

public class AiSummaryResponse implements Serializable {
    private boolean hasSummary;
    private String summaryText;

    @Serial
    private static final long serialVersionUID = 1L;

    public AiSummaryResponse() {
    }

    public AiSummaryResponse(boolean hasSummary, String summaryText) {
        this.hasSummary = hasSummary;
        this.summaryText = summaryText;
    }


    public boolean isHasSummary() {
        return hasSummary;
    }

    public void setHasSummary(boolean hasSummary) {
        this.hasSummary = hasSummary;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
}
