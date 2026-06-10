package com.javaweb.customException;

import com.javaweb.common.exception.BaseException;
import com.javaweb.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidDataException extends BaseException {
    public InvalidDataException(String e) {
        super(ErrorCode.INVALID_REQUEST, e, HttpStatus.BAD_REQUEST);
    }
}
