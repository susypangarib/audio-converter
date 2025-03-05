package com.audio.converter.controller;

import com.audio.converter.model.BaseResponse;
import com.audio.converter.model.ResponseCode;
import com.audio.converter.util.BusinessLogicException;
import com.audio.converter.util.RequestValidationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ConstraintViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrorController {
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(constructBaseResponse(ResponseCode.BIND_ERROR.getCode(), errorMessage));
    }
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<BaseResponse> handleNotFoundException(NoHandlerFoundException ex) {
        return ResponseEntity.badRequest()
                .body(constructBaseResponse(ResponseCode.BIND_ERROR.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<BaseResponse> handleBusinessLogicException(BusinessLogicException ex) {
        return ResponseEntity.internalServerError()
                .body(constructBaseResponse(ex.getCode(),ex.getMessage()));
    }

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<BaseResponse> handleBusinessLogicException(RequestValidationException ex) {
        return ResponseEntity.badRequest()
                .body(constructBaseResponse(ex.getCode(),ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<BaseResponse> handleIOException(IOException ex) {
        return ResponseEntity.badRequest()
                .body(constructBaseResponse(ResponseCode.CONVERSION_FAILED.getCode(),ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<BaseResponse> handleMissingFileException(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(constructBaseResponse(ResponseCode.BIND_ERROR.getCode(),ex.getMessage()));
    }

    private BaseResponse constructBaseResponse(String code, String message){
        return BaseResponse.builder()
                        .code(code)
                        .message(message)
                        .build();
    }
}
