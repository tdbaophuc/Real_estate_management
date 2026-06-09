package com.javaweb.storage.exception;

import com.javaweb.common.exception.BaseException;
import com.javaweb.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class FileUploadException extends BaseException {
    public FileUploadException(String message) {
        super(ErrorCode.FILE_UPLOAD_ERROR, message, HttpStatus.BAD_REQUEST);
    }

    public FileUploadException(String message, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
