package com.javaweb.ai.repository;

import com.javaweb.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    @EntityGraph(attributePaths = {"createdBy", "messages"})
    Optional<AiConversation> findWithMessagesById(Long id);
}
