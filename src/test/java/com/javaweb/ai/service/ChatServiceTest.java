package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.ChatMessageRequest;
import com.javaweb.ai.dto.ChatSessionCreateRequest;
import com.javaweb.ai.dto.ChatSessionResponse;
import com.javaweb.ai.entity.AiConversation;
import com.javaweb.ai.entity.AiMessage;
import com.javaweb.ai.enums.AiMessageRole;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.repository.AiConversationRepository;
import com.javaweb.ai.repository.AiMessageRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
    private final AiMessageRepository messageRepository = mock(AiMessageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final ChatService service = new ChatService(
            conversationRepository,
            messageRepository,
            userRepository,
            aiService,
            new ChatbotGuardrails(),
            new ObjectMapper()
    );

    @Test
    void shouldCreateChatSessionForActor() {
        User user = user(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            ReflectionTestUtils.setField(conversation, "id", 100L);
            return conversation;
        });

        ChatSessionResponse response = service.createSession(
                new ChatSessionCreateRequest("Find apartment"),
                customer()
        );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Find apartment");
        assertThat(response.createdById()).isEqualTo(10L);
        assertThat(response.messages()).isEmpty();
    }

    @Test
    void shouldSendMessageAndUseAiReply() {
        User user = user(10L);
        AiConversation conversation = conversation(user);
        when(conversationRepository.findWithMessagesById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(100L))
                .thenReturn(
                        List.of(new AiMessage(AiMessageRole.USER, "Toi muon mua can ho Quan 1")),
                        List.of(
                                new AiMessage(AiMessageRole.USER, "Toi muon mua can ho Quan 1"),
                                assistant("Ban nen cho biet ngan sach va so phong mong muon", AiRequestStatus.SUCCESS)
                        )
                );
        when(aiService.complete(any())).thenReturn(new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                "test-provider",
                "test-model",
                "Ban nen cho biet ngan sach va so phong mong muon",
                "STOP",
                20,
                30,
                50,
                null
        ));

        ChatSessionResponse response = service.sendMessage(
                100L,
                new ChatMessageRequest("Toi muon mua can ho Quan 1"),
                customer()
        );

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(1).role()).isEqualTo(AiMessageRole.ASSISTANT);
        assertThat(response.messages().get(1).content()).contains("ngan sach");

        ArgumentCaptor<AiCompletionRequest> captor = ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(aiService).complete(captor.capture());
        assertThat(captor.getValue().operation()).isEqualTo("CHAT_MESSAGE");
        assertThat(captor.getValue().systemPrompt()).contains("Guardrails");
        assertThat(captor.getValue().metadataJson()).contains("\"conversationId\":100");
    }

    @Test
    void shouldUseGuardrailFallbackWhenAiIsSkipped() {
        User user = user(10L);
        AiConversation conversation = conversation(user);
        when(conversationRepository.findWithMessagesById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(100L))
                .thenReturn(
                        List.of(new AiMessage(AiMessageRole.USER, "Tu van phap ly hop dong nay")),
                        List.of(
                                new AiMessage(AiMessageRole.USER, "Tu van phap ly hop dong nay"),
                                assistant("fallback", AiRequestStatus.SKIPPED)
                        )
                );
        when(aiService.complete(any())).thenReturn(AiCompletionResponse.skipped(
                "noop",
                "not-configured",
                "AI provider is disabled or API key is not configured"
        ));

        ChatSessionResponse response = service.sendMessage(
                100L,
                new ChatMessageRequest("Tu van phap ly hop dong nay"),
                customer()
        );

        assertThat(response.messages()).hasSize(2);
        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).saveAndFlush(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getContent()).contains("phap ly");
        assertThat(messageCaptor.getAllValues().get(1).getAiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        verify(aiService).complete(any());
    }

    private AiMessage assistant(String content, AiRequestStatus status) {
        AiMessage message = new AiMessage(AiMessageRole.ASSISTANT, content);
        message.setAiResult(status, "test", "model", null);
        return message;
    }

    private AiConversation conversation(User user) {
        AiConversation conversation = new AiConversation(user, "Find apartment");
        ReflectionTestUtils.setField(conversation, "id", 100L);
        return conversation;
    }

    private User user(Long id) {
        User user = new User("customer@example.test", "password", "Customer");
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AuthUserPrincipal customer() {
        return new AuthUserPrincipal(
                10L,
                "customer@example.test",
                "password",
                "Customer",
                UserStatus.ACTIVE,
                null,
                List.of("CUSTOMER"),
                List.of(),
                List.of()
        );
    }
}
