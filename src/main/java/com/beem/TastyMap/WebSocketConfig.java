package com.beem.TastyMap;

import com.beem.TastyMap.websocket.AuthWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AuthWebSocketHandler authWebSocketHandler;

    public WebSocketConfig(AuthWebSocketHandler authWebSocketHandler) {
        this.authWebSocketHandler = authWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(authWebSocketHandler, "/ws/auth/{deviceId}")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        String path = request.getURI().getPath();
                        String deviceId = path.substring(path.lastIndexOf('/') + 1);
                        attributes.put("deviceId", deviceId);
                        return true;
                    }
                });
    }
}