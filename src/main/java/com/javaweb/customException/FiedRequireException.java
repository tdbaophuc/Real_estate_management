package com.javaweb.customException;

import com.javaweb.common.exception.BaseException;
import com.javaweb.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class FiedRequireException extends BaseException {
    public FiedRequireException(String e) {
        super(ErrorCode.VALIDATION_ERROR, e, HttpStatus.BAD_REQUEST);
    }
}
