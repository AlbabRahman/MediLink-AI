package com.medilinkai.service;

import com.medilinkai.model.ChatMessage;
import com.medilinkai.model.User;
import com.medilinkai.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository chatRepo;

    public ChatService(ChatMessageRepository chatRepo) {
        this.chatRepo = chatRepo;
    }

    public ChatMessage save(User sender, User receiver, String content) {
        return chatRepo.save(new ChatMessage(sender, receiver, content));
    }

    public List<ChatMessage> history(Long userA, Long userB) {
        return chatRepo.findConversation(userA, userB);
    }
}
