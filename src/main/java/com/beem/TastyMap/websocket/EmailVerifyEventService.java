package com.beem.TastyMap.websocket;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailVerifyEventService {
    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageSource messageSource;

    public EmailVerifyEventService(WebSocketSessionManager webSocketSessionManager, MessageSource messageSource) {
        this.webSocketSessionManager = webSocketSessionManager;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public void EmailVerified(String deviceId) throws IOException {
        SecurityEventDTO event = new SecurityEventDTO(
                WebSocketEventType.EMAIL_VERIFIED,
                getMessage("websocket.email.verified")
        );

        webSocketSessionManager.sendEvent(deviceId, event);
    }
}