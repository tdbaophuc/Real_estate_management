package com.javaweb.controllerAdvice;

import com.javaweb.model.ErrorResponseDTO;
import com.javaweb.customException.FiedRequireException;
import com.javaweb.customException.InvalidDataException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Object> handleArithmeticException(
            ArithmeticException ex, WebRequest request) {

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError(ex.getMessage());
        List<String> detail = new ArrayList<>();
        detail.add("khong the chia cho 0");
        errorResponseDTO.setDetails(detail);

        return new ResponseEntity<>(errorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FiedRequireException.class)
    public ResponseEntity<Object> handleFiedRequireException(
            FiedRequireException ex, WebRequest request) {

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError(ex.getMessage());
        List<String> detail = new ArrayList<>();
        detail.add("Kiem tra lai da dien day du cac thong tin chua");
        errorResponseDTO.setDetails(detail);

        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<Object> handleInvalidDataException(
            InvalidDataException ex, WebRequest request) {

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError(ex.getMessage());
        List<String> detail = new ArrayList<>();
        detail.add("so hang phai lon hon bang 0");
        errorResponseDTO.setDetails(detail);

        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }
}
