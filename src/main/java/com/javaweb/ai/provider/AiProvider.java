package com.javaweb.ai.provider;

import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;

public interface AiProvider {
    String name();

    AiCompletionResponse complete(AiCompletionRequest request);
}
