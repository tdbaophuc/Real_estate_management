package com.javaweb.ai.service;

import com.javaweb.ai.config.AiProperties;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.entity.AiRequestLog;
import com.javaweb.ai.entity.AiRequestLog.AiCompletionResponseSnapshot;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.provider.AiProvider;
import com.javaweb.ai.provider.NoopAiProvider;
import com.javaweb.ai.repository.AiRequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DefaultAiService implements AiService {
    private static final String NOT_CONFIGURED_MESSAGE =
            "AI provider is disabled or API key is not configured";

    private final AiProperties properties;
    private final Map<String, AiProvider> providers;
    private final AiRequestLogRepository logRepository;

    public DefaultAiService(
            AiProperties properties,
            List<AiProvider> providers,
            AiRequestLogRepository logRepository
    ) {
        this.properties = properties;
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        provider -> provider.name().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));
        this.logRepository = logRepository;
    }

    @Override
    @Transactional
    public AiCompletionResponse complete(AiCompletionRequest request) {
        AiProvider provider = resolveProvider();
        AiRequestLog log = new AiRequestLog(
                provider.name(),
                properties.model(),
                request.operation(),
                request.systemPrompt(),
                request.userPrompt(),
                request.metadataJson(),
                request.referenceType(),
                request.referenceId()
        );
        long start = System.nanoTime();

        AiCompletionResponse response;
        if (!properties.enabled() || !properties.hasApiKey()) {
            response = AiCompletionResponse.skipped(
                    provider.name(),
                    properties.model(),
                    NOT_CONFIGURED_MESSAGE
            );
        } else {
            response = callProvider(provider, request);
        }

        log.complete(snapshot(response), elapsedMillis(start));
        logRepository.saveAndFlush(log);
        return response;
    }

    private AiProvider resolveProvider() {
        AiProvider provider = providers.get(properties.provider().toLowerCase(Locale.ROOT));
        if (provider != null) {
            return provider;
        }
        return providers.getOrDefault(NoopAiProvider.PROVIDER_NAME, providers.values().iterator().next());
    }

    private AiCompletionResponse callProvider(
            AiProvider provider,
            AiCompletionRequest request
    ) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> provider.complete(request))
                    .get(timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            return AiCompletionResponse.failed(
                    AiRequestStatus.TIMEOUT,
                    provider.name(),
                    properties.model(),
                    "AI provider timed out"
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return AiCompletionResponse.failed(
                    AiRequestStatus.FAILED,
                    provider.name(),
                    properties.model(),
                    "AI provider call was interrupted"
            );
        } catch (ExecutionException exception) {
            return AiCompletionResponse.failed(
                    AiRequestStatus.FAILED,
                    provider.name(),
                    properties.model(),
                    limitError(rootMessage(exception))
            );
        }
    }

    private Duration timeout() {
        return properties.timeout().isNegative() || properties.timeout().isZero()
                ? Duration.ofSeconds(10)
                : properties.timeout();
    }

    private AiCompletionResponseSnapshot snapshot(AiCompletionResponse response) {
        return new AiCompletionResponseSnapshot(
                response.status(),
                response.content(),
                response.finishReason(),
                response.promptTokens(),
                response.completionTokens(),
                response.totalTokens(),
                response.errorMessage()
        );
    }

    private long elapsedMillis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private String rootMessage(ExecutionException exception) {
        Throwable cause = exception.getCause();
        return cause == null || cause.getMessage() == null
                ? "AI provider call failed"
                : cause.getMessage();
    }

    private String limitError(String message) {
        return message.substring(0, Math.min(message.length(), 4000));
    }
}
