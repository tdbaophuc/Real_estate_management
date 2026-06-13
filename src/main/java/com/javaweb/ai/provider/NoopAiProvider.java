package com.javaweb.ai.provider;

import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class NoopAiProvider implements AiProvider {
    public static final String PROVIDER_NAME = "noop";

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public AiCompletionResponse complete(AiCompletionRequest request) {
        return new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                PROVIDER_NAME,
                "noop",
                "",
                "NOOP",
                null,
                null,
                null,
                null
        );
    }
}
