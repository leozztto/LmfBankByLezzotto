package com.lezztto.LmfBank.exception.handler;

import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.exception.DocumentNumberDuplicateException;
import com.lezztto.LmfBank.exception.domain.ApiError;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(
            AccountNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(DocumentNumberDuplicateException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DocumentNumberDuplicateException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_EXISTS", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiError> handleAccountStatus(
            AccountStatusException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_BLOCKED", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleBalance(
            InsufficientBalanceException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .status(status.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }
}
