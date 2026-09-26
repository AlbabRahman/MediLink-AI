package com.medilinkai.socket;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Pushes live stock changes to every connected patient.
 * The pharmacist saves a stock update through a normal form (no JS);
 * StockService then calls broadcastStockChanged() and all open patient
 * pages receive: {"type":"stock","pharmacy":"...","medicine":"...","qty":40}
 */
@Component
public class StockWebSocketHandler extends TextWebSocketHandler {

    private final SessionRegistry registry;

    public StockWebSocketHandler(@Qualifier("stockRegistry") SessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            registry.add(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            registry.remove(userId);
        }
    }

    public void broadcastStockChanged(String pharmacy, String medicine, int qty) {
        String json = "{\"type\":\"stock\",\"pharmacy\":\"" + ChatWebSocketHandler.escape(pharmacy)
                + "\",\"medicine\":\"" + ChatWebSocketHandler.escape(medicine)
                + "\",\"qty\":" + qty + "}";
        registry.broadcast(json);
    }

    private Long getUserId(WebSocketSession session) {
        Object id = session.getAttributes().get("userId");
        return (id instanceof Long l) ? l : null;
    }
}
