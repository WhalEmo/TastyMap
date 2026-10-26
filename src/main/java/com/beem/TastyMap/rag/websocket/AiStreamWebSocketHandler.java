package com.beem.TastyMap.rag.websocket;

import com.beem.TastyMap.rag.data.response.AiStreamPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class AiStreamWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AiStreamWebSocketHandler.class);

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractAuthenticatedUserId(session);
        if (userId == null) {
            log.warn("Yetkisiz AI WebSocket bağlantı denemesi reddedildi.");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("AI WebSocket Bağlantısı Açıldı - User ID: {}, Session: {}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractAuthenticatedUserId(session);
        if (userId != null && userSessions.containsKey(userId)) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
            log.info("AI WebSocket Bağlantısı Kapandı - User ID: {}", userId);
        }
    }

    public void sendToUser(Long userId, AiStreamPayload payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Kullanıcıya ({}) AI stream mesajı gönderilemedi: {}", userId, e.getMessage());
        }
    }

    /**
     * JwtAuthenticationFilter tarafından doğrulanan userId'yi doğrudan Principal üzerinden alır!
     */
    private Long extractAuthenticatedUserId(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}