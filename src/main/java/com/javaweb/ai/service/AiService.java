package com.javaweb.ai.service;

import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;

public interface AiService {
    AiCompletionResponse complete(AiCompletionRequest request);
}
