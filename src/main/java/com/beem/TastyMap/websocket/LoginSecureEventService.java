package com.beem.TastyMap.websocket;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
public class LoginSecureEventService {
    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageSource messageSource;

    public LoginSecureEventService(WebSocketSessionManager webSocketSessionManager, MessageSource messageSource) {
        this.webSocketSessionManager = webSocketSessionManager;
        this.messageSource = messageSource;
    }
    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
    public void loginApproved(String deviceId) throws IOException {

        SecurityEventDTO event =
                new SecurityEventDTO(
                        WebSocketEventType.LOGIN_APPROVED,
                        getMessage("websocket.login.approved")
                );

        webSocketSessionManager.sendEvent(deviceId, event);
    }


    public void loginRejected(String deviceId)
            throws IOException {

        SecurityEventDTO event = new SecurityEventDTO(
                        WebSocketEventType.LOGIN_REJECTED,
                getMessage("websocket.login.rejected")
                );

        webSocketSessionManager.sendEvent(deviceId, event);
    }
}
