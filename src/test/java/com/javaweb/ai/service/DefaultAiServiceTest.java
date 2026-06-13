package com.javaweb.ai.service;

import com.javaweb.ai.config.AiProperties;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.provider.AiProvider;
import com.javaweb.ai.provider.NoopAiProvider;
import com.javaweb.ai.repository.AiRequestLogRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAiServiceTest {

    @Test
    void shouldSkipProviderCallWhenApiKeyIsMissing() {
        AiRequestLogRepository repository = mock(AiRequestLogRepository.class);
        AiProvider provider = mock(AiProvider.class);
        when(provider.name()).thenReturn("test");
        DefaultAiService service = new DefaultAiService(
                new AiProperties(false, "test", "", "model-a", Duration.ofSeconds(1)),
                List.of(provider, new NoopAiProvider()),
                repository
        );

        AiCompletionResponse response = service.complete(request());

        assertThat(response.status()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.errorMessage()).contains("API key is not configured");
        verify(provider, never()).complete(any());
        verify(repository).saveAndFlush(any());
    }

    @Test
    void shouldReturnProviderResponseAndLogSuccess() {
        AiRequestLogRepository repository = mock(AiRequestLogRepository.class);
        AiProvider provider = mock(AiProvider.class);
        when(provider.name()).thenReturn("test");
        when(provider.complete(any())).thenReturn(new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                "test",
                "model-a",
                "Generated content",
                "STOP",
                10,
                20,
                30,
                null
        ));
        DefaultAiService service = new DefaultAiService(
                new AiProperties(true, "test", "secret", "model-a", Duration.ofSeconds(1)),
                List.of(provider, new NoopAiProvider()),
                repository
        );

        AiCompletionResponse response = service.complete(request());

        assertThat(response.status()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(response.content()).isEqualTo("Generated content");
        verify(provider).complete(any());
        verify(repository).saveAndFlush(any());
    }

    @Test
    void shouldConvertProviderExceptionToFailedResponse() {
        AiRequestLogRepository repository = mock(AiRequestLogRepository.class);
        AiProvider provider = mock(AiProvider.class);
        when(provider.name()).thenReturn("test");
        when(provider.complete(any())).thenThrow(new IllegalStateException("provider down"));
        DefaultAiService service = new DefaultAiService(
                new AiProperties(true, "test", "secret", "model-a", Duration.ofSeconds(1)),
                List.of(provider, new NoopAiProvider()),
                repository
        );

        AiCompletionResponse response = service.complete(request());

        assertThat(response.status()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(response.errorMessage()).isEqualTo("provider down");
        verify(repository).saveAndFlush(any());
    }

    @Test
    void shouldConvertSlowProviderToTimeoutResponse() {
        AiRequestLogRepository repository = mock(AiRequestLogRepository.class);
        AiProvider provider = mock(AiProvider.class);
        when(provider.name()).thenReturn("test");
        when(provider.complete(any())).thenAnswer(invocation -> {
            Thread.sleep(200);
            return new AiCompletionResponse(
                    AiRequestStatus.SUCCESS,
                    "test",
                    "model-a",
                    "too late",
                    "STOP",
                    null,
                    null,
                    null,
                    null
            );
        });
        DefaultAiService service = new DefaultAiService(
                new AiProperties(true, "test", "secret", "model-a", Duration.ofMillis(10)),
                List.of(provider, new NoopAiProvider()),
                repository
        );

        AiCompletionResponse response = service.complete(request());

        assertThat(response.status()).isEqualTo(AiRequestStatus.TIMEOUT);
        assertThat(response.errorMessage()).contains("timed out");
        verify(repository).saveAndFlush(any());
    }

    private AiCompletionRequest request() {
        return new AiCompletionRequest(
                "LISTING_DESCRIPTION",
                "Write safely",
                "Create listing content",
                "listing",
                10L,
                "{\"source\":\"test\"}"
        );
    }
}
