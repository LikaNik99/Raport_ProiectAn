package com.blackjack.config;

import com.blackjack.websocket.BlackjackWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BlackjackWebSocketHandler blackjackWebSocketHandler;

    @Autowired
    public WebSocketConfig(BlackjackWebSocketHandler blackjackWebSocketHandler) {
        this.blackjackWebSocketHandler = blackjackWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(blackjackWebSocketHandler, "/blackjack")
                .setAllowedOrigins("*");  // In productie, specifica originile exacte
    }
}
