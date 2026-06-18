package com.beem.TastyMap.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthWebSocketHandler extends TextWebSocketHandler {


    private final WebSocketSessionManager sessionManager;

    public AuthWebSocketHandler(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String deviceId = getDeviceId(session);

        sessionManager.addSession(deviceId, session);
    }


    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String deviceId = getDeviceId(session);

        sessionManager.removeSession(deviceId);
    }


    private String getDeviceId(WebSocketSession session) {

        return (String) session.getAttributes().get("deviceId");
    }
}
