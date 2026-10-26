package com.beem.TastyMap.rag.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CostTracker {

    private static final Logger log = LoggerFactory.getLogger(CostTracker.class);

    private static final double INPUT_PER_MILLION = 0.30;
    private static final double OUTPUT_PER_MILLION = 0.70;
    private static final double USD_TO_TRY = 45.0;

    public record ExecutionCost(long inputTokens, long outputTokens, double costUsd) {
        public double costTry() {
            return costUsd * USD_TO_TRY;
        }
    }

    public static ExecutionCost calculate(long promptTokens, long completionTokens) {
        double inputCost = (promptTokens / 1_000_000.0) * INPUT_PER_MILLION;
        double outputCost = (completionTokens / 1_000_000.0) * OUTPUT_PER_MILLION;
        double totalCost = inputCost + outputCost;
        return new ExecutionCost(promptTokens, completionTokens, totalCost);
    }

    public static void logSummary(String stageName, long promptTokens, long completionTokens) {
        ExecutionCost cost = calculate(promptTokens, completionTokens);
        log.info("📊 [{}] Girdi: {} token, Çıktı: {} token | Maliyet: ${} (~{} TL)",
                stageName,
                cost.inputTokens(),
                cost.outputTokens(),
                String.format("%.6f", cost.costUsd()),
                String.format("%.4f", cost.costTry()));
    }
}