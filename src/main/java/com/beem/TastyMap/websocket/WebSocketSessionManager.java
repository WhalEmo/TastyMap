package com.beem.TastyMap.websocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class WebSocketSessionManager {
    private final ObjectMapper objectMapper;

    public WebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    public void addSession(
            String deviceId,
            WebSocketSession session
    ) {
        sessions.put(deviceId, session);
    }


    public void removeSession(String deviceId) {
        sessions.remove(deviceId);
    }


    public void sendEvent(String deviceId, SecurityEventDTO event) throws IOException {

        WebSocketSession session = sessions.get(deviceId);
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(event);
            session.sendMessage(new TextMessage(json));
        }
    }
}