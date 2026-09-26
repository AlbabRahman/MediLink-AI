package com.medilinkai.controller;

import com.medilinkai.model.User;
import com.medilinkai.model.UserRole;
import com.medilinkai.service.ChatService;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feature 4: live patient <-> pharmacist chat.
 * The page loads history from the database; new messages arrive by WebSocket.
 */
@Controller
public class ChatController {

    private final UserService userService;
    private final ChatService chatService;

    public ChatController(UserService userService, ChatService chatService) {
        this.userService = userService;
        this.chatService = chatService;
    }

    /** Pick someone to talk to: patients see pharmacists and vice versa. */
    @GetMapping("/chat")
    public String contacts(HttpSession session, Model model) {
        User me = currentUser(session);
        UserRole otherRole = (me.getRole() == UserRole.PHARMACIST) ? UserRole.PATIENT : UserRole.PHARMACIST;
        List<User> contacts = userService.findByRole(otherRole);
        model.addAttribute("me", me);
        model.addAttribute("contacts", contacts);
        return "chat-contacts";
    }

    /** Conversation page with history + live socket updates. */
    @GetMapping("/chat/{partnerId}")
    public String conversation(@PathVariable Long partnerId, HttpSession session, Model model) {
        User me = currentUser(session);
        User partner = userService.findById(partnerId).orElse(null);
        if (partner == null) {
            return "redirect:/chat";
        }
        model.addAttribute("me", me);
        model.addAttribute("partner", partner);
        model.addAttribute("messages", chatService.history(me.getId(), partnerId));
        return "chat-room";
    }

    private User currentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return userService.findById(userId).orElseThrow();
    }
}
