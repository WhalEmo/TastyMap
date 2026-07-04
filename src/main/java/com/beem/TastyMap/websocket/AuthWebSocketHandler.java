package com.beem.TastyMap.websocket;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;


@Component
public class AuthWebSocketHandler extends TextWebSocketHandler {


    private final WebSocketSessionManager sessionManager;

    public AuthWebSocketHandler(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String deviceId = getDeviceId(session);
        System.out.println("Cihaz bağlandıauthwebsockethandler: " + deviceId);

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
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();

        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        super.handleMessage(session, message);
    }
    private String getDeviceId(WebSocketSession session) {

        return (String) session.getAttributes().get("deviceId");
    }
}
