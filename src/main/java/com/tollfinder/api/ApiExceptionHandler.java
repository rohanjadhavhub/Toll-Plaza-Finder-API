package com.tollfinder.api;

import com.tollfinder.application.FinderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalidInput(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid source or destination pincode");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException exception) {
        String message = "Source and destination pincodes cannot be the same".equals(exception.getMessage())
                ? exception.getMessage() : "Invalid source or destination pincode";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Request body must use application/json");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMethod(HttpRequestMethodNotSupportedException exception) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Use POST for toll plaza searches");
    }

    @ExceptionHandler(FinderException.class)
    ResponseEntity<ApiError> applicationError(FinderException exception) {
        var status = switch (exception.reason()) {
            case INVALID_PINCODE -> HttpStatus.BAD_REQUEST;
            case NO_ROUTE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case UPSTREAM_FAILURE -> HttpStatus.BAD_GATEWAY;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
        };
        return error(status, exception.getMessage());
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<ApiError> databaseUnavailable(Exception exception) {
        log.warn("Database operation failed: {}", exception.getClass().getSimpleName());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Toll plaza data is temporarily unavailable");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        // Do not log upstream URLs: geocoding URLs contain the server API key.
        log.error("Request failed: {}", exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(new ApiError(message));
    }
}
