package com.beem.TastyMap.websocket;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailVerifyEventService {
    private final WebSocketSessionManager webSocketSessionManager;

    public EmailVerifyEventService(WebSocketSessionManager webSocketSessionManager) {
        this.webSocketSessionManager = webSocketSessionManager;
    }
    public void EmailVerified(String deviceId) throws IOException {

        SecurityEventDTO event =
                new SecurityEventDTO(
                        WebSocketEventType.EMAIL_VERIFIED,
                        "Email Doğrulandı"
                );

        webSocketSessionManager.sendEvent(deviceId, event);
    }
}
