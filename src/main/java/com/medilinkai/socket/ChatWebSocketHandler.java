package com.medilinkai.socket;

import com.medilinkai.model.ChatMessage;
import com.medilinkai.model.User;
import com.medilinkai.model.UserRole;
import com.medilinkai.service.ChatService;
import com.medilinkai.service.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Live patient <-> pharmacist chat.
 * Browser sends:   {"to": 2, "text": "Is Napa available?"}
 * Server pushes:   {"from": 1, "fromName": "Rahim", "text": "...", "at": "16:30"}
 *
 * Single-user testing: if the recipient is OFFLINE, a demo auto-reply from
 * the pharmacist is scheduled on a background thread (multithreading demo),
 * so one person with one browser still sees a live incoming push.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String AUTO_REPLY =
            "[Auto demo reply] The pharmacist is offline right now - your message was saved. "
            + "For a real two-way chat, open the pharmacist account in a second (Incognito) browser window.";

    private final SessionRegistry registry;
    private final ChatService chatService;
    private final UserService userService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ChatWebSocketHandler(@Qualifier("chatRegistry") SessionRegistry registry,
                                ChatService chatService, UserService userService) {
        this.registry = registry;
        this.chatService = chatService;
        this.userService = userService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            registry.add(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = getUserId(session);
        if (senderId == null) {
            return;
        }

        // very small JSON parsing: {"to": 2, "text": "hello"}
        String payload = message.getPayload();
        Long toId = extractLong(payload, "to");
        String text = extractString(payload, "text");
        if (toId == null || text == null || text.isBlank()) {
            return;
        }

        Optional<User> sender = userService.findById(senderId);
        Optional<User> receiver = userService.findById(toId);
        if (sender.isEmpty() || receiver.isEmpty()) {
            return;
        }

        // 1) save to database so history survives restarts
        chatService.save(sender.get(), receiver.get(), text.trim());

        // 2) push instantly to receiver (and back to sender so their UI updates)
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String json = "{\"type\":\"chat\",\"from\":" + senderId
                + ",\"fromName\":\"" + escape(sender.get().getName()) + "\""
                + ",\"text\":\"" + escape(text.trim()) + "\""
                + ",\"at\":\"" + time + "\"}";
        boolean receiverOnline = registry.sendTo(toId, json);
        registry.sendTo(senderId, json);

        // 3) recipient offline? schedule an automatic demo reply on a background
        //    thread so a single user can still test the live push (demo mode)
        if (!receiverOnline && receiver.get().getRole() == UserRole.PHARMACIST) {
            scheduler.schedule(() -> autoReply(receiver.get(), sender.get()), 2, TimeUnit.SECONDS);
        }
    }

    /** Persists and pushes an automatic demo reply from the pharmacist. */
    private void autoReply(User pharmacist, User patient) {
        ChatMessage saved = chatService.save(pharmacist, patient, AUTO_REPLY);
        String time = saved.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        String json = "{\"type\":\"chat\",\"from\":" + pharmacist.getId()
                + ",\"fromName\":\"" + escape(pharmacist.getName())
                + " (auto)\",\"text\":\"" + escape(AUTO_REPLY) + "\""
                + ",\"at\":\"" + time + "\"}";
        registry.sendTo(patient.getId(), json);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            registry.remove(userId);
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object id = session.getAttributes().get("userId");
        return (id instanceof Long l) ? l : null;
    }

    // ---- tiny helpers to avoid pulling in a JSON library for 2 fields ----

    static Long extractLong(String json, String key) {
        String value = extractRaw(json, key);
        try {
            return value == null ? null : Long.parseLong(value.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String extractString(String json, String key) {
        String value = extractRaw(json, key);
        return value == null ? null : value.trim();
    }

    private static String extractRaw(String json, String key) {
        String marker = "\"" + key + "\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int colon = json.indexOf(':', start + marker.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && (json.charAt(i) == ' ')) {
            i++;
        }
        if (i < json.length() && json.charAt(i) == '"') {
            int end = json.indexOf('"', i + 1);
            return end < 0 ? null : json.substring(i + 1, end);
        }
        int end = i;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(i, end);
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
