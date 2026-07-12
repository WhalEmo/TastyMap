package com.beem.TastyMap.websocket;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PasswordEventService {
    private final WebSocketSessionManager webSocketSessionManager;

    public PasswordEventService(WebSocketSessionManager webSocketSessionManager) {
        this.webSocketSessionManager = webSocketSessionManager;
    }
    public void PasswordChange(String deviceId) throws IOException {

        SecurityEventDTO event =
                new SecurityEventDTO(
                        WebSocketEventType.PASSWORD_CHANGE,
                        "Şifre değiştirildi"
                );

        webSocketSessionManager.sendEvent(deviceId, event);
    }
}
