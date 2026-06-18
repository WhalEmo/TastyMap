package com.beem.TastyMap.websocket;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LoginSecureEventService {
    private final WebSocketSessionManager webSocketSessionManager;

    public LoginSecureEventService(WebSocketSessionManager webSocketSessionManager) {
        this.webSocketSessionManager = webSocketSessionManager;
    }
    public void loginApproved(String deviceId) throws IOException {

        SecurityEventDTO event =
                new SecurityEventDTO(
                        WebSocketEventType.LOGIN_APPROVED,
                        "Giriş onaylandı"
                );

        webSocketSessionManager.sendEvent(deviceId, event);
    }


    public void loginRejected(String deviceId)
            throws IOException {

        SecurityEventDTO event = new SecurityEventDTO(
                        WebSocketEventType.LOGIN_REJECTED,
                        "Giriş reddedildi"
                );

        webSocketSessionManager.sendEvent(deviceId, event);
    }
}
