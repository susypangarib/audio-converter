package com.audio.converter.controller;

import com.audio.converter.model.BaseResponse;
import com.audio.converter.model.ResponseCode;
import com.audio.converter.util.BusinessLogicException;
import com.audio.converter.util.RequestValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorControllerTest {

    @InjectMocks
    private ErrorController errorController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Invalid input");

        Set<ConstraintViolation<?>> violations = Collections.singleton(violation);
        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", violations);

        ResponseEntity<BaseResponse> response = errorController.handleConstraintViolationException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseCode.BIND_ERROR.getCode(), response.getBody().getCode());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    void testHandleNotFoundException() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/invalid-endpoint", null);

        ResponseEntity<BaseResponse> response = errorController.handleNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseCode.BIND_ERROR.getCode(), response.getBody().getCode());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleBusinessLogicException() {
        String errorCode = "BUSINESS_ERROR";
        String errorMessage = "Business logic failed";
        BusinessLogicException ex = new BusinessLogicException(errorCode, errorMessage);

        ResponseEntity<BaseResponse> response = errorController.handleBusinessLogicException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(errorCode, response.getBody().getCode());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleRequestValidationException() {
        String errorCode = "VALIDATION_ERROR";
        String errorMessage = "Invalid request";
        RequestValidationException ex = new RequestValidationException(errorCode, errorMessage);

        ResponseEntity<BaseResponse> response = errorController.handleBusinessLogicException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorCode, response.getBody().getCode());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleIOException() {
        IOException ex = new IOException("File not found");

        ResponseEntity<BaseResponse> response = errorController.handleIOException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseCode.CONVERSION_FAILED.getCode(), response.getBody().getCode());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleMissingFileException() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("file");

        ResponseEntity<BaseResponse> response = errorController.handleMissingFileException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseCode.BIND_ERROR.getCode(), response.getBody().getCode());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleMaxSizeException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50_000_000);

        ResponseEntity<?> response = errorController.handleMaxSizeException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(response.getBody() instanceof BaseResponse);

        BaseResponse baseResponse = (BaseResponse) response.getBody();
        assertEquals(ResponseCode.FILE_SIZE_EXCEEDED.getCode(), baseResponse.getCode());
        assertEquals(ResponseCode.FILE_SIZE_EXCEEDED.getMessage(), baseResponse.getMessage());
    }

    @Test
    void testHandleSocketTimeoutException() {
        String errorMessage = "Connection timed out";
        SocketTimeoutException ex = new SocketTimeoutException(errorMessage);

        ResponseEntity<BaseResponse> response = errorController.handleSocketTimeoutException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseCode.CANNOT_CONNECT_TO_GCP.getCode(), response.getBody().getCode());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleRuntimeException() {
        RuntimeException ex = new RuntimeException("Unexpected error occurred");
        ResponseEntity<BaseResponse> response = errorController.handleRuntimeException(ex);
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ResponseCode.SYSTEM_ERROR.getCode(), response.getBody().getCode());
        assertEquals(ResponseCode.SYSTEM_ERROR.getMessage(), response.getBody().getMessage());
    }
}