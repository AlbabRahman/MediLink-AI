package com.medilinkai.api;

import com.medilinkai.model.ChatMessage;
import com.medilinkai.model.User;
import com.medilinkai.model.UserRole;
import com.medilinkai.service.ChatService;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Live consultation chat for the portal. One shared consultation room:
 * every signed-in patient/pharmacist sees the same thread, so any
 * patient-pharmacist pair is always in sync.
 */
@RestController
public class ApiChatController {

    private final UserService userService;
    private final ChatService chatService;
    private final ApiEventBus eventBus;

    public ApiChatController(UserService userService, ChatService chatService, ApiEventBus eventBus) {
        this.userService = userService;
        this.chatService = chatService;
        this.eventBus = eventBus;
    }

    /** The counterpart for the global consultation chat. */
    private User counterpart(User me) {
        UserRole other = me.getRole() == UserRole.PHARMACIST ? UserRole.PATIENT : UserRole.PHARMACIST;
        List<User> candidates = userService.findByRole(other);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private Map<String, Object> dto(ChatMessage m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("senderName", m.getSender() == null ? "User" : m.getSender().getName());
        out.put("senderRole", m.getSender() == null ? "PATIENT" : m.getSender().getRole().name());
        out.put("content", m.getContent());
        out.put("sentAt", m.getSentAt() == null ? "" : m.getSentAt().toString());
        return out;
    }

    @GetMapping("/api/chat/messages")
    public Map<String, Object> messages(HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("messages", List.of());
        }
        // Shared room: everyone reads the same ordered thread.
        List<ChatMessage> all = chatService.findAllOrdered();
        return Map.of("messages", all.stream().map(this::dto).collect(Collectors.toList()));
    }

    public record SendRequest(String senderId, String senderName, String senderRole,
                              String receiverId, String content) {
    }

    @PostMapping("/api/chat/send")
    public Map<String, Object> send(@RequestBody SendRequest req, HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("status", "ERROR", "message", "Not signed in.");
        }
        User partner = counterpart(me);
        if (partner == null) {
            return Map.of("status", "ERROR", "message", "No pharmacist available yet.");
        }
        chatService.save(me, partner, req.content());
        eventBus.publish("CHAT_MESSAGE: New message from " + me.getName()
                + " (" + me.getRole().name() + ")");
        return Map.of("status", "SUCCESS");
    }
}
