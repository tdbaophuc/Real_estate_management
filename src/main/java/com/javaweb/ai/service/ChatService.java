package com.javaweb.ai.service;

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

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private static final String OPERATION = "CHAT_MESSAGE";

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ChatbotGuardrails guardrails;
    private final ChatContextBuilder contextBuilder;

    public ChatService(
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            UserRepository userRepository,
            AiService aiService,
            ChatbotGuardrails guardrails,
            ChatContextBuilder contextBuilder
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.guardrails = guardrails;
        this.contextBuilder = contextBuilder;
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
        return toSessionResponse(
                conversationRepository.saveAndFlush(conversation),
                List.of(),
                List.of()
        );
    }

    @Transactional
    public ChatSessionResponse createGuestSession(
            ChatSessionCreateRequest request,
            String guestSessionId
    ) {
        String resolvedGuestSessionId = normalizeGuestSessionId(guestSessionId);
        AiConversation conversation = new AiConversation(
                resolvedGuestSessionId,
                request.title() == null ? "Guest AI chat session" : request.title()
        );
        return toSessionResponse(
                conversationRepository.saveAndFlush(conversation),
                List.of(),
                List.of()
        );
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
        ChatContextBuilder.ChatContext context = contextBuilder.build(request.content(), history, actor);
        if (guardrails.needsProfessionalReferral(request.content())) {
            AiMessage assistantMessage = new AiMessage(AiMessageRole.ASSISTANT, limit(context.fallbackReply(), 8000));
            assistantMessage.setAiResult(
                    AiRequestStatus.SKIPPED,
                    "guardrails",
                    "guardrails",
                    null
            );
            conversation.addMessage(assistantMessage);
            messageRepository.saveAndFlush(assistantMessage);
            conversationRepository.saveAndFlush(conversation);
            return toSessionResponse(
                    conversation,
                    messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId),
                    List.of()
            );
        }

        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                OPERATION,
                context.systemPrompt(),
                context.userPrompt(),
                "ai_conversation",
                conversation.getId(),
                context.metadataJson()
        ));
        String reply = aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())
                ? aiResponse.content().trim()
                : context.fallbackReply();
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

        return toSessionResponse(
                conversation,
                messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId),
                context.suggestedListings()
        );
    }

    @Transactional
    public ChatSessionResponse sendGuestMessage(
            Long sessionId,
            ChatMessageRequest request,
            String guestSessionId
    ) {
        AiConversation conversation = requireAccessibleGuestConversation(sessionId, guestSessionId);
        if (conversation.getStatus() != AiConversationStatus.OPEN) {
            throw new BusinessException("Chat session is closed");
        }

        AiMessage userMessage = new AiMessage(AiMessageRole.USER, request.content());
        conversation.addMessage(userMessage);
        messageRepository.saveAndFlush(userMessage);

        List<AiMessage> history = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId);
        ChatContextBuilder.ChatContext context = contextBuilder.buildGuest(
                request.content(),
                history,
                conversation.getGuestSessionId()
        );
        if (guardrails.needsProfessionalReferral(request.content())) {
            AiMessage assistantMessage = new AiMessage(AiMessageRole.ASSISTANT, limit(context.fallbackReply(), 8000));
            assistantMessage.setAiResult(
                    AiRequestStatus.SKIPPED,
                    "guardrails",
                    "guardrails",
                    null
            );
            conversation.addMessage(assistantMessage);
            messageRepository.saveAndFlush(assistantMessage);
            conversationRepository.saveAndFlush(conversation);
            return toSessionResponse(
                    conversation,
                    messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId),
                    List.of()
            );
        }

        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                OPERATION,
                context.systemPrompt(),
                context.userPrompt(),
                "ai_conversation",
                conversation.getId(),
                context.metadataJson()
        ));
        String reply = aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())
                ? aiResponse.content().trim()
                : context.fallbackReply();
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

        return toSessionResponse(
                conversation,
                messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId),
                context.suggestedListings()
        );
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(Long sessionId, AuthUserPrincipal actor) {
        AiConversation conversation = requireAccessibleConversation(sessionId, actor);
        List<AiMessage> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(conversation, messages, List.of());
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getGuestSession(Long sessionId, String guestSessionId) {
        AiConversation conversation = requireAccessibleGuestConversation(sessionId, guestSessionId);
        List<AiMessage> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(conversation, messages, List.of());
    }

    private AiConversation requireAccessibleConversation(
            Long sessionId,
            AuthUserPrincipal actor
    ) {
        AiConversation conversation = conversationRepository.findWithMessagesById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (conversation.getCreatedBy() == null) {
            throw new AccessDeniedException("Guest chat sessions require guest access");
        }
        if (!isManagerOrAdmin(actor)
                && !conversation.getCreatedBy().getId().equals(actor.id())) {
            throw new AccessDeniedException("Users can only access their own chat sessions");
        }
        return conversation;
    }

    private AiConversation requireAccessibleGuestConversation(
            Long sessionId,
            String guestSessionId
    ) {
        String normalizedGuestSessionId = requireGuestSessionId(guestSessionId);
        AiConversation conversation = conversationRepository.findWithMessagesById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (conversation.getCreatedBy() != null
                || !normalizedGuestSessionId.equals(conversation.getGuestSessionId())) {
            throw new ResourceNotFoundException("Chat session not found");
        }
        return conversation;
    }

    private ChatSessionResponse toSessionResponse(
            AiConversation conversation,
            List<AiMessage> messages,
            List<com.javaweb.listing.dto.PublicListingResponse> suggestedListings
    ) {
        User createdBy = conversation.getCreatedBy();
        return new ChatSessionResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                createdBy == null ? null : createdBy.getId(),
                createdBy == null ? null : createdBy.getFullName(),
                conversation.getGuestSessionId(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                messages.stream().map(this::toMessageResponse).toList(),
                suggestedListings
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
        if (actor == null) {
            return false;
        }
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

    private String normalizeGuestSessionId(String guestSessionId) {
        String normalized = guestSessionId == null ? null : guestSessionId.trim();
        if (normalized == null || normalized.isBlank()) {
            return UUID.randomUUID().toString();
        }
        if (normalized.length() > 100) {
            throw new BusinessException("guest session id must not exceed 100 characters");
        }
        return normalized;
    }

    private String requireGuestSessionId(String guestSessionId) {
        String normalized = guestSessionId == null ? null : guestSessionId.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException("X-Guest-Session-Id header is required");
        }
        if (normalized.length() > 100) {
            throw new BusinessException("guest session id must not exceed 100 characters");
        }
        return normalized;
    }
}
