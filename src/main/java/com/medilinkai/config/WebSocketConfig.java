package com.medilinkai.config;

import com.medilinkai.socket.ChatWebSocketHandler;
import com.medilinkai.socket.StockWebSocketHandler;
import com.medilinkai.socket.UserHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatHandler;
    private final StockWebSocketHandler stockHandler;

    public WebSocketConfig(ChatWebSocketHandler chatHandler, StockWebSocketHandler stockHandler) {
        this.chatHandler = chatHandler;
        this.stockHandler = stockHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat")
                .addInterceptors(new UserHandshakeInterceptor())
                .setAllowedOrigins("*");
        registry.addHandler(stockHandler, "/ws/stock")
                .addInterceptors(new UserHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
