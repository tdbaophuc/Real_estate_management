package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.ListingDescriptionRequest;
import com.javaweb.ai.dto.ListingDescriptionResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.FurnitureStatus;
import com.javaweb.property.enums.PropertyLegalStatus;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingDescriptionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final ListingRepository listingRepository = mock(ListingRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final ListingDescriptionService service = new ListingDescriptionService(
            propertyRepository,
            listingRepository,
            aiService,
            new ListingDescriptionPromptBuilder(objectMapper),
            new ListingDescriptionFallbackBuilder(),
            objectMapper
    );

    @Test
    void shouldParseAiJsonIntoListingDescriptionResponse() {
        Property property = property();
        when(propertyRepository.findActiveDetailsById(99L)).thenReturn(Optional.of(property));
        when(aiService.complete(any())).thenReturn(new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                "test-provider",
                "test-model",
                """
                        {
                          "title": "Can ho trung tam quan 1",
                          "shortDescription": "Can ho sang trong gan tien ich.",
                          "fullDescription": "Can ho co dien tich rong, phu hop gia dinh can vi tri trung tam.",
                          "seoKeywords": ["can ho quan 1", "bat dong san trung tam"],
                          "socialMediaCaption": "Can ho quan 1 dang mo ban"
                        }
                        """,
                "STOP",
                100,
                80,
                180,
                null
        ));

        ListingDescriptionResponse response = service.generate(
                new ListingDescriptionRequest(99L, null, List.of("gan metro"), "friendly", "vi"),
                manager()
        );

        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(response.title()).isEqualTo("Can ho trung tam quan 1");
        assertThat(response.shortDescription()).contains("sang trong");
        assertThat(response.fullDescription()).contains("gia dinh");
        assertThat(response.seoKeywords()).containsExactly("can ho quan 1", "bat dong san trung tam");
        assertThat(response.socialMediaCaption()).contains("quan 1");

        ArgumentCaptor<AiCompletionRequest> requestCaptor = ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(aiService).complete(requestCaptor.capture());
        AiCompletionRequest aiRequest = requestCaptor.getValue();
        assertThat(aiRequest.operation()).isEqualTo("LISTING_DESCRIPTION");
        assertThat(aiRequest.userPrompt()).contains("Source data");
        assertThat(aiRequest.metadataJson()).contains("gan metro", "Can ho", "Quan 1");
    }

    @Test
    void shouldReturnFallbackWhenAiIsSkipped() {
        Property property = property();
        when(propertyRepository.findActiveDetailsById(99L)).thenReturn(Optional.of(property));
        when(aiService.complete(any())).thenReturn(AiCompletionResponse.skipped(
                "noop",
                "not-configured",
                "AI provider is disabled or API key is not configured"
        ));

        ListingDescriptionResponse response = service.generate(
                new ListingDescriptionRequest(99L, null, List.of("view song"), null, null),
                manager()
        );

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.provider()).isEqualTo("noop");
        assertThat(response.errorMessage()).contains("API key is not configured");
        assertThat(response.title()).contains("Can ho");
        assertThat(response.shortDescription()).contains("Quan 1");
        assertThat(response.fullDescription()).contains("view song");
        assertThat(response.seoKeywords()).contains("view song");
    }

    @Test
    void shouldReturnFallbackWhenAiResponseCannotBeParsed() {
        Property property = property();
        when(propertyRepository.findActiveDetailsById(99L)).thenReturn(Optional.of(property));
        when(aiService.complete(any())).thenReturn(new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                "test-provider",
                "test-model",
                "not-json",
                "STOP",
                null,
                null,
                null,
                null
        ));

        ListingDescriptionResponse response = service.generate(
                new ListingDescriptionRequest(99L, null, List.of(), null, null),
                manager()
        );

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(response.errorMessage()).contains("not valid listing-description JSON");
        assertThat(response.fullDescription()).contains("Bat dong san phu hop");
    }

    private Property property() {
        Province province = new Province("HCM", "TP Ho Chi Minh");
        District district = new District(province, "Q1", "Quan 1");
        Ward ward = new Ward(district, "BN", "Ben Nghe");
        province.addDistrict(district);
        district.addWard(ward);
        Address address = new Address(province, "1 Dong Khoi");
        address.setDistrict(district);
        address.setWard(ward);
        address.setFullAddress("1 Dong Khoi, Ben Nghe, Quan 1, TP Ho Chi Minh");

        Property property = new Property(
                "PROP-AI-001",
                "Can ho Quan 1",
                new PropertyType("APT", "Can ho"),
                address,
                null,
                PropertyPurpose.SALE
        );
        property.setPrice(new BigDecimal("5500000000"));
        property.setFloorArea(new BigDecimal("82.5"));
        property.setBedrooms(2);
        property.setBathrooms(2);
        property.setLegalStatus(PropertyLegalStatus.PINK_BOOK);
        property.setFurnitureStatus(FurnitureStatus.FULLY_FURNISHED);
        return property;
    }

    private AuthUserPrincipal manager() {
        return new AuthUserPrincipal(
                1L,
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
