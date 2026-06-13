package com.javaweb.ai.exception;

import com.javaweb.common.exception.BaseException;
import com.javaweb.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class AiProviderException extends BaseException {
    public AiProviderException(String message) {
        super(ErrorCode.AI_PROVIDER_ERROR, message, HttpStatus.BAD_GATEWAY);
    }

    public AiProviderException(String message, Throwable cause) {
        super(ErrorCode.AI_PROVIDER_ERROR, message, HttpStatus.BAD_GATEWAY, cause);
    }
}
