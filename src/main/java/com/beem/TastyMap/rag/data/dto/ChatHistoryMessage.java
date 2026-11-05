package com.beem.TastyMap.rag.data.dto;

import com.beem.TastyMap.rag.data.enums.MessageRole;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record ChatHistoryMessage(
        MessageRole role,             // Tip güvenli: USER veya ASSISTANT
        String content,               // Mesaj metni
        Instant timestamp,            // Mesajın zaman damgası
        Map<String, Object> metadata  // Ek veriler (placeId vb.)
) implements Serializable {

    // Compact Constructor: Güvenlik ve varsayılan değerler
    public ChatHistoryMessage {
        if (role == null) {
            role = MessageRole.USER;
        }
        if (content == null) {
            content = "";
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
    }

    // Kullanımı kolaylaştıran statik yardımcı metotlar
    public static ChatHistoryMessage user(String content) {
        return new ChatHistoryMessage(MessageRole.USER, content, Instant.now(), Collections.emptyMap());
    }

    public static ChatHistoryMessage assistant(String content) {
        return new ChatHistoryMessage(MessageRole.ASSISTANT, content, Instant.now(), Collections.emptyMap());
    }

    public static ChatHistoryMessage assistant(String content, Map<String, Object> metadata) {
        return new ChatHistoryMessage(MessageRole.ASSISTANT, content, Instant.now(), metadata);
    }
}