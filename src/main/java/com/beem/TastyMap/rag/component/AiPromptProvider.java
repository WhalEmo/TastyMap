package com.beem.TastyMap.rag.component;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class AiPromptProvider {

    private final String systemRules;

    public AiPromptProvider(
            @Value("classpath:prompts/restaurant-assistant-system-v2.md")
            Resource promptResource
    ) throws java.io.IOException {
        this.systemRules = StreamUtils.copyToString(
                promptResource.getInputStream(),
                StandardCharsets.UTF_8
        );
    }

    public String getSystemRules() {
        return systemRules;
    }
}