package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.ChatMessageRequest;
import com.javaweb.ai.dto.ChatMessageResponse;
import com.javaweb.ai.dto.ChatSessionCreateRequest;
import com.javaweb.ai.dto.ChatSessionResponse;
import com.javaweb.ai.entity.AiConversation;
import com.javaweb.ai.entity.AiMessage;
import com.javaweb.ai.enums.AiConversationStatus;
import com.javaweb.ai.enums.AiMessageRole;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.repository.AiConversationRepository;
import com.javaweb.ai.repository.AiMessageRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private static final String OPERATION = "CHAT_MESSAGE";

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ChatbotGuardrails guardrails;
    private final ObjectMapper objectMapper;

    public ChatService(
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            UserRepository userRepository,
            AiService aiService,
            ChatbotGuardrails guardrails,
            ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.guardrails = guardrails;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatSessionResponse createSession(
            ChatSessionCreateRequest request,
            AuthUserPrincipal actor
    ) {
        User creator = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        AiConversation conversation = new AiConversation(
                creator,
                request.title() == null ? "AI chat session" : request.title()
        );
        return toSessionResponse(conversationRepository.saveAndFlush(conversation), List.of());
    }

    @Transactional
    public ChatSessionResponse sendMessage(
            Long sessionId,
            ChatMessageRequest request,
            AuthUserPrincipal actor
    ) {
        AiConversation conversation = requireAccessibleConversation(sessionId, actor);
        if (conversation.getStatus() != AiConversationStatus.OPEN) {
            throw new BusinessException("Chat session is closed");
        }

        AiMessage userMessage = new AiMessage(AiMessageRole.USER, request.content());
        conversation.addMessage(userMessage);
        messageRepository.saveAndFlush(userMessage);

        List<AiMessage> history = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId);
        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                OPERATION,
                guardrails.systemPrompt(),
                buildUserPrompt(request.content(), history),
                "ai_conversation",
                conversation.getId(),
                metadataJson(conversation, actor, history)
        ));
        String reply = aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())
                ? aiResponse.content().trim()
                : guardrails.fallbackReply(request.content(), aiResponse.errorMessage());
        AiMessage assistantMessage = new AiMessage(AiMessageRole.ASSISTANT, limit(reply, 8000));
        assistantMessage.setAiResult(
                aiResponse.status(),
                aiResponse.provider(),
                aiResponse.model(),
                limit(aiResponse.errorMessage(), 1000)
        );
        conversation.addMessage(assistantMessage);
        messageRepository.saveAndFlush(assistantMessage);
        conversationRepository.saveAndFlush(conversation);

        return getSession(sessionId, actor);
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(Long sessionId, AuthUserPrincipal actor) {
        AiConversation conversation = requireAccessibleConversation(sessionId, actor);
        List<AiMessage> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(conversation, messages);
    }

    private AiConversation requireAccessibleConversation(
            Long sessionId,
            AuthUserPrincipal actor
    ) {
        AiConversation conversation = conversationRepository.findWithMessagesById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (!isManagerOrAdmin(actor)
                && !conversation.getCreatedBy().getId().equals(actor.id())) {
            throw new AccessDeniedException("Users can only access their own chat sessions");
        }
        return conversation;
    }

    private String buildUserPrompt(String currentMessage, List<AiMessage> history) {
        return """
                Conversation history:
                %s

                Current user message:
                %s
                """.formatted(historyText(history), currentMessage);
    }

    private String historyText(List<AiMessage> history) {
        return history.stream()
                .limit(20)
                .map(message -> message.getRole().name() + ": " + message.getContent())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("(no previous messages)");
    }

    private String metadataJson(
            AiConversation conversation,
            AuthUserPrincipal actor,
            List<AiMessage> history
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversationId", conversation.getId());
        payload.put("actorId", actor.id());
        payload.put("actorRoles", actor.roles());
        payload.put("messageCount", history.size());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build AI chat metadata", exception);
        }
    }

    private ChatSessionResponse toSessionResponse(
            AiConversation conversation,
            List<AiMessage> messages
    ) {
        User createdBy = conversation.getCreatedBy();
        return new ChatSessionResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                createdBy.getId(),
                createdBy.getFullName(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                messages.stream().map(this::toMessageResponse).toList()
        );
    }

    private ChatMessageResponse toMessageResponse(AiMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getAiStatus(),
                message.getProvider(),
                message.getModel(),
                message.getErrorMessage(),
                message.getCreatedAt()
        );
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
