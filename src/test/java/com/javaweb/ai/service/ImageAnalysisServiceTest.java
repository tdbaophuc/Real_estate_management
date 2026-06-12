package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.ImageAnalysisRequest;
import com.javaweb.ai.dto.ImageAnalysisResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.provider.NoopImageAnalysisProvider;
import com.javaweb.ai.repository.AiImageAnalysisRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyImage;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyImageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageAnalysisServiceTest {
    private final PropertyImageRepository imageRepository = mock(PropertyImageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiImageAnalysisRepository analysisRepository = mock(AiImageAnalysisRepository.class);
    private final ImageAnalysisService service = new ImageAnalysisService(
            imageRepository,
            userRepository,
            List.of(new NoopImageAnalysisProvider()),
            analysisRepository,
            new ObjectMapper()
    );

    @Test
    void shouldReturnMetadataFallbackWhenVisionProviderUnavailable() {
        User agent = user();
        PropertyImage image = image(agent);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(imageRepository.findWithAnalysisDetailsById(99L)).thenReturn(Optional.of(image));

        ImageAnalysisResponse response = service.analyze(
                new ImageAnalysisRequest(List.of(99L)),
                manager()
        );

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().issues()).contains("Missing alt text or caption");
        verify(analysisRepository).save(any());
    }

    private PropertyImage image(User agent) {
        Province province = new Province("HCM", "TP Ho Chi Minh");
        Address address = new Address(province, "1 Dong Khoi");
        Property property = new Property(
                "PROP-IMG",
                "Can ho",
                new PropertyType("APT", "Can ho"),
                address,
                agent,
                PropertyPurpose.SALE
        );
        ReflectionTestUtils.setField(property, "id", 7L);
        PropertyImage image = new PropertyImage(agent, "http://localhost/image.jpg");
        ReflectionTestUtils.setField(image, "id", 99L);
        image.setMimeType("image/jpeg");
        image.setFileSize(10_000L);
        property.addImage(image);
        return image;
    }

    private User user() {
        User user = new User("agent@example.test", "password", "Agent");
        ReflectionTestUtils.setField(user, "id", 10L);
        return user;
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
