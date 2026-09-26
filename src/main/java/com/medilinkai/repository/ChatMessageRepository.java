package com.medilinkai.repository;

import com.medilinkai.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m where (m.sender.id = :a and m.receiver.id = :b) " +
           "or (m.sender.id = :b and m.receiver.id = :a) order by m.sentAt")
    List<ChatMessage> findConversation(@Param("a") Long userA, @Param("b") Long userB);
}
