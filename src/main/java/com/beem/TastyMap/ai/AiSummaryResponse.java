package com.beem.TastyMap.ai;

public class AiSummaryResponse {
    private boolean hasSummary;
    private String summaryText;

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
