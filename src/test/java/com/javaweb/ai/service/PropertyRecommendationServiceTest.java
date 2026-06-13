package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.PropertyRecommendationRequest;
import com.javaweb.ai.dto.PropertyRecommendationResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.repository.AiRecommendationRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.customer.repository.CustomerRequirementRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyRecommendationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerRequirementRepository requirementRepository = mock(CustomerRequirementRepository.class);
    private final ListingRepository listingRepository = mock(ListingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final AiRecommendationRepository recommendationRepository = mock(AiRecommendationRepository.class);
    private final PropertyRecommendationService service = new PropertyRecommendationService(
            customerRepository,
            requirementRepository,
            listingRepository,
            userRepository,
            aiService,
            new PropertyRecommendationPromptBuilder(objectMapper),
            new PropertyRecommendationScorer(),
            new ListingMapper(),
            recommendationRepository,
            objectMapper
    );

    @Test
    void shouldReturnRuleBasedFallbackWhenAiIsSkipped() {
        User agent = user(10L);
        Customer customer = customer(agent);
        CustomerRequirement requirement = requirement();
        Listing listing = listing(99L, agent);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(requirementRepository.findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(requirement));
        when(listingRepository.findRecommendationCandidates(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(List.of(listing)));
        when(aiService.complete(any())).thenReturn(AiCompletionResponse.skipped(
                "noop",
                "not-configured",
                "AI provider is disabled or API key is not configured"
        ));
        when(listingRepository.getReferenceById(99L)).thenReturn(listing);

        PropertyRecommendationResponse response = service.recommend(
                1L,
                new PropertyRecommendationRequest(5, 20, "gan trung tam view song", "vi"),
                manager()
        );

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().getFirst().listing().id()).isEqualTo(99L);
        assertThat(response.recommendations().getFirst().matchScore()).isGreaterThan(70);
        assertThat(response.recommendations().getFirst().reason()).contains("budget", "district");
        verify(recommendationRepository).saveAll(any());
    }

    @Test
    void shouldUseAiRankingWhenResponseIsValid() {
        User agent = user(10L);
        Customer customer = customer(agent);
        Listing listing = listing(99L, agent);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(requirementRepository.findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(requirement()));
        when(listingRepository.findRecommendationCandidates(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(List.of(listing)));
        when(aiService.complete(any())).thenReturn(new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                "test-provider",
                "test-model",
                """
                        {
                          "recommendations": [
                            {
                              "listingId": 99,
                              "matchScore": 92,
                              "reason": "Best fit for location, budget and space.",
                              "suggestedAction": "Call the customer and schedule a viewing."
                            }
                          ]
                        }
                        """,
                "STOP",
                100,
                80,
                180,
                null
        ));
        when(listingRepository.getReferenceById(99L)).thenReturn(listing);

        PropertyRecommendationResponse response = service.recommend(
                1L,
                new PropertyRecommendationRequest(5, 20, null, "vi"),
                manager()
        );

        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.provider()).isEqualTo("test-provider");
        assertThat(response.recommendations().getFirst().matchScore()).isEqualTo(92);
        assertThat(response.recommendations().getFirst().reason()).contains("Best fit");

        ArgumentCaptor<AiCompletionRequest> captor = ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(aiService).complete(captor.capture());
        assertThat(captor.getValue().operation()).isEqualTo("PROPERTY_RECOMMENDATION");
        assertThat(captor.getValue().metadataJson()).contains("\"listingId\":99");
    }

    private User user(Long id) {
        User user = new User("agent@example.test", "password", "Agent");
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Customer customer(User agent) {
        Customer customer = new Customer("CUS-AI-001", "Nguyen Van A", agent);
        customer.setAssignedAgent(agent);
        customer.setNotes("Needs a central apartment");
        ReflectionTestUtils.setField(customer, "id", 1L);
        return customer;
    }

    private CustomerRequirement requirement() {
        Province province = province();
        District district = district(province);
        PropertyType type = propertyType();
        CustomerRequirement requirement = new CustomerRequirement(ListingPurpose.SALE);
        requirement.setPropertyType(type);
        requirement.setProvince(province);
        requirement.setDistrict(district);
        requirement.setMinBudget(new BigDecimal("4000000000"));
        requirement.setMaxBudget(new BigDecimal("6000000000"));
        requirement.setMinArea(new BigDecimal("70"));
        requirement.setMinBedrooms(2);
        requirement.setDescription("gan trung tam");
        return requirement;
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
                "PROP-AI-REC",
                "Can ho trung tam Quan 1",
                propertyType(),
                address,
                agent,
                PropertyPurpose.SALE
        );
        ReflectionTestUtils.setField(property, "id", 88L);
        property.setFloorArea(new BigDecimal("82"));
        property.setBedrooms(2);
        property.setBathrooms(2);
        Listing listing = new Listing(
                "LST-AI-REC",
                property,
                agent,
                "Can ho Quan 1 view song",
                "can-ho-quan-1-view-song",
                "Can ho gan trung tam, view song, tien ich day du.",
                ListingPurpose.SALE
        );
        ReflectionTestUtils.setField(listing, "id", id);
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setVisibility(ListingVisibility.PUBLIC);
        listing.setAskingPrice(new BigDecimal("5500000000"));
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

    private AuthUserPrincipal manager() {
        return new AuthUserPrincipal(
                10L,
                "manager@example.test",
                "password",
                "Manager",
                UserStatus.ACTIVE,
                null,
                List.of("MANAGER"),
                List.of(),
                List.of()
        );
    }
}
