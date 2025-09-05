package com.app.entitlement.handler;

import com.app.entitlement.constants.CommonConstants;
import com.app.entitlement.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.ws.rs.ProcessingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for controllers.
 */
@ControllerAdvice(basePackages = "com.app.entitlement.controller")
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private void logException(Exception ex) {
        logger.error("{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
    }

    /**
     * Handle JAX-RS client connection issues.
     */
    @ExceptionHandler(ProcessingException.class)
    public ResponseEntity<Map<String, String>> handleProcessingException(ProcessingException ex) {
        logger.error("JAX-RS ProcessingException: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity("Error communicating with authentication service", HttpStatus.BAD_GATEWAY);
    }

    /**
     * Handle custom application exceptions.
     */
    @ExceptionHandler({UserAlreadyExistsException.class,RoleAssignedException.class})
    public ResponseEntity<Map<String, String>> handleConflictExceptions(RuntimeException ex) {
        logger.warn("Conflict: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Handle bad request exceptions.
     */
    @ExceptionHandler({InvalidPasswordException.class})
    public ResponseEntity<Map<String, String>> handleBadRequestExceptions(RuntimeException ex) {
        logger.warn("Bad Request: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle internal server errors.
     */
    @ExceptionHandler({UserCreationException.class})
    public ResponseEntity<Map<String, String>> handleInternalErrors(RuntimeException ex) {
        logger.error("Internal Error: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle not found exceptions.
     */
    @ExceptionHandler({RoleNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFoundExceptions(RuntimeException ex) {
        logger.warn("Not Found: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback handler for all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex, WebRequest request) {
        logException(ex);
        return buildResponseEntity("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Build a response entity with consistent structure.
     */
    private ResponseEntity<Map<String, String>> buildResponseEntity(String message, HttpStatus status) {
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put(CommonConstants.STATUS, CommonConstants.FAILURE);
        return new ResponseEntity<>(response, status);
    }

}
