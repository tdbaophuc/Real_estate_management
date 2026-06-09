package com.javaweb.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
