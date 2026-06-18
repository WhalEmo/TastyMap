package com.beem.TastyMap.websocket;
public class SecurityEventDTO {

    private WebSocketEventType type;

    private String message;


    public SecurityEventDTO(
            WebSocketEventType type,
            String message
    ) {
        this.type = type;
        this.message = message;
    }


    public WebSocketEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public void setType(WebSocketEventType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
