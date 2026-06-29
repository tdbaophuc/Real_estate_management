package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.listing.mapper.ListingMapper;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.PropertyPurpose;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
    private final AiMessageRepository messageRepository = mock(AiMessageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ListingRepository listingRepository = mock(ListingRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final ChatService service = new ChatService(
            conversationRepository,
            messageRepository,
            userRepository,
            aiService,
            new ChatbotGuardrails(),
            new ChatContextBuilder(
                    listingRepository,
                    new ListingMapper(),
                    new ObjectMapper(),
                    new ChatbotGuardrails()
            )
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
        assertThat(response.suggestedListings()).isEmpty();
    }

    @Test
    void shouldSendMessageAndUseAiReplyWithRealSuggestions() {
        User user = user(10L);
        AiConversation conversation = conversation(user);
        Listing candidate = listing(99L, user);
        when(conversationRepository.findWithMessagesById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(100L))
                .thenReturn(
                        List.of(new AiMessage(AiMessageRole.USER, "Toi muon thue can ho 2 phong ngu")),
                        List.of(
                                new AiMessage(AiMessageRole.USER, "Toi muon thue can ho 2 phong ngu"),
                                assistant("Ban nen cho biet ngan sach va so phong mong muon", AiRequestStatus.SUCCESS)
                        )
                );
        when(listingRepository.findRecommendationCandidates(
                eq(ListingPurpose.RENT),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(List.of(candidate)));
        when(listingRepository.findAllByStatusAndVisibilityAndDeletedAtIsNull(
                eq(ListingStatus.PUBLISHED),
                eq(ListingVisibility.PUBLIC),
                any()
        )).thenReturn(new PageImpl<>(List.of(candidate)));
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
                new ChatMessageRequest("Toi muon thue can ho 2 phong ngu"),
                customer()
        );

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(1).role()).isEqualTo(AiMessageRole.ASSISTANT);
        assertThat(response.messages().get(1).content()).contains("ngan sach");

        ArgumentCaptor<com.javaweb.ai.dto.AiCompletionRequest> captor =
                ArgumentCaptor.forClass(com.javaweb.ai.dto.AiCompletionRequest.class);
        verify(aiService).complete(captor.capture());
        assertThat(captor.getValue().operation()).isEqualTo("CHAT_MESSAGE");
        assertThat(captor.getValue().systemPrompt()).contains("Only use the candidate listings");
        assertThat(captor.getValue().metadataJson()).contains("\"candidateIds\":[99]");
        assertThat(response.suggestedListings()).isNotEmpty();
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
                                assistant(
                                        "Toi co the ho tro thong tin bat dong san o muc tham khao, nhung voi cau hoi phap ly, thue, vay von hoac tai chinh chuyen sau, ban nen trao doi truc tiep voi nhan vien phu trach hoac chuyen gia phu hop.",
                                        AiRequestStatus.SKIPPED
                                )
                        )
                );
        when(listingRepository.findRecommendationCandidates(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(List.of()));
        when(listingRepository.findAllByStatusAndVisibilityAndDeletedAtIsNull(
                eq(ListingStatus.PUBLISHED),
                eq(ListingVisibility.PUBLIC),
                any()
        )).thenReturn(new PageImpl<>(List.of()));
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
        assertThat(response.messages().get(1).content()).contains("phap ly");
        assertThat(response.messages().get(1).aiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.suggestedListings()).isEmpty();
        verify(aiService, never()).complete(any());
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

    private Listing listing(Long id, User agent) {
        Province province = province();
        District district = district(province);
        Ward ward = new Ward(district, "BN", "Ben Nghe");
        ReflectionTestUtils.setField(ward, "id", 3L);
        Address address = new Address(province, "1 Dong Khoi");
        address.setDistrict(district);
        address.setWard(ward);
        address.setFullAddress("1 Dong Khoi, Ben Nghe, Quan 1, TP Ho Chi Minh");
        Property property = new Property(
                "PROP-CHAT-001",
                "Can ho trung tam Quan 1",
                propertyType(),
                address,
                agent,
                PropertyPurpose.RENT
        );
        ReflectionTestUtils.setField(property, "id", 88L);
        property.setFloorArea(new BigDecimal("82"));
        property.setBedrooms(2);
        property.setBathrooms(2);
        Listing listing = new Listing(
                "LST-CHAT-001",
                property,
                agent,
                "Can ho Quan 1 view song",
                "can-ho-quan-1-view-song",
                "can ho gan trung tam",
                ListingPurpose.RENT
        );
        ReflectionTestUtils.setField(listing, "id", id);
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setVisibility(ListingVisibility.PUBLIC);
        listing.setAskingPrice(new BigDecimal("15000000"));
        listing.setCurrency("VND");
        return listing;
    }

    private Province province() {
        Province province = new Province("HCM", "TP Ho Chi Minh");
        ReflectionTestUtils.setField(province, "id", 1L);
        return province;
    }

    private District district(Province province) {
        District district = new District(province, "Q1", "Quan 1");
        ReflectionTestUtils.setField(district, "id", 2L);
        return district;
    }

    private PropertyType propertyType() {
        PropertyType type = new PropertyType("APT", "Can ho");
        ReflectionTestUtils.setField(type, "id", 5L);
        return type;
    }
}
