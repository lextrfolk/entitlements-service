package com.app.entitlement.handler;



import com.app.entitlement.constants.CommonConstants;
import com.app.entitlement.exception.UserAlreadyExistsException;
import com.app.entitlement.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import javax.ws.rs.ProcessingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for controller.
 */
@ControllerAdvice(basePackages = "com.app.entitlement.controller")
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Log the exception message.
     *
     * @param ex the exception
     */
    private void logException(Exception ex) {
        logger.error("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
    }


    /**
     * Handle ProcessingException from JAX-RS client (e.g., Keycloak connection issues).
     *
     * @param ex the ProcessingException
     * @return the response entity
     */
    @ExceptionHandler(ProcessingException.class)
    public ResponseEntity<Map<String, String>> handleProcessingException(ProcessingException ex) {
        logger.error("JAX-RS ProcessingException: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity("Error communicating with authentication service", HttpStatus.BAD_GATEWAY);
    }

    /**
     * Handles UserAlreadyExistsException thrown by controllers.
     *
     * @param ex the UserAlreadyExistsException
     * @return a ResponseEntity with CONFLICT status and error details
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        logger.warn("User already exists: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity(ex.getMessage(), HttpStatus.CONFLICT);
    }


    /**
     * Handles UserNotFoundException thrown by controllers.
     *
     * @param ex the UserNotFoundException
     * @return a ResponseEntity with NOT_FOUND status and error details
     */
    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        logException(ex);
        return buildResponseEntity(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handle validation exceptions.
     *
     * @param ex the exception
     * @return the response entity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
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
     * Handle global exception.
     *
     * @param ex      the exception
     * @param request the request
     * @return the response entity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex, WebRequest request) {
        logException(ex);
        return buildResponseEntity("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * Build a response entity with a consistent structure.
     *
     * @param message the message
     * @param status  the HTTP status
     * @return the response entity
     */
    private ResponseEntity<Map<String, String>> buildResponseEntity(String message, HttpStatus status) {
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put(CommonConstants.STATUS,CommonConstants.FAILURE);
        return new ResponseEntity<>(response, status);
    }

    private HttpStatus safeResolveHttpStatus(HttpStatusCode statusCode) {
        try {
            return HttpStatus.resolve(statusCode.value());
        } catch (Exception e) {
            logger.error("Error while resolving HttpStatus from ResponseStatusException", e);
        }
        return null;
    }

}