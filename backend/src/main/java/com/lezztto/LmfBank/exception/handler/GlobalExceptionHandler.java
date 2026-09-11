package com.lezztto.LmfBank.exception.handler;

import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.exception.DocumentNumberDuplicateException;
import com.lezztto.LmfBank.auth.exception.AccountAlreadyLinkedException;
import com.lezztto.LmfBank.auth.exception.AppUserNotFoundException;
import com.lezztto.LmfBank.auth.exception.ForbiddenActionException;
import com.lezztto.LmfBank.auth.exception.InvalidCredentialsException;
import com.lezztto.LmfBank.exception.domain.ApiError;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler(AppUserNotFoundException.class)
    public ResponseEntity<ApiError> handleAppUserNotFound(
            AppUserNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountAlreadyLinkedException.class)
    public ResponseEntity<ApiError> handleAccountAlreadyLinked(
            AccountAlreadyLinkedException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_LINKED", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ApiError> handleForbiddenAction(
            ForbiddenActionException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request);
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid value"
            );
        }

        ApiError error = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("VALIDATION_ERROR")
                .message("Falha de validação")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Corpo da requisição inválido", request);
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
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
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
