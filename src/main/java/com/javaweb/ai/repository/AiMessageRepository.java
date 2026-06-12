package com.javaweb.ai.repository;

import com.javaweb.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findAllByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
