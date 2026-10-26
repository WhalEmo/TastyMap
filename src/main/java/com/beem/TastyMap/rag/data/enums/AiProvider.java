package com.beem.TastyMap.rag.data.enums;

public enum AiProvider {
    GROQ,
    OLLAMA;

    public static AiProvider fromString(String value) {
        if (value == null) return GROQ;
        try {
            return AiProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GROQ;
        }
    }
}