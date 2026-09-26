package com.medilinkai.socket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of online users' WebSocket sessions.
 * Many browser tabs connect at the same time, each handled on its own
 * server thread, so this map must be concurrent (multithreading demo).
 *
 * One registry per channel (chat, stock) so a user's chat socket and
 * stock socket never overwrite or cross-talk with each other.
 */
public class SessionRegistry {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public void remove(Long userId) {
        sessions.remove(userId);
    }

    /** Send a text message to one online user. Returns false when offline. */
    public boolean sendTo(Long userId, String json) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // synchronize: two threads must not write to one socket at once
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
                return true;
            } catch (IOException e) {
                sessions.remove(userId);
            }
        }
        return false;
    }

    /** Send a text message to every online user. */
    public void broadcast(String json) {
        for (Long userId : sessions.keySet()) {
            sendTo(userId, json);
        }
    }
}
