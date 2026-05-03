package com.lezztto.LmfBank.account.exception;

import com.lezztto.LmfBank.account.domain.dto.ErrorResponse;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<?> handleAccountNotFound(AccountNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "ACCOUNT_NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(DocumentNumberDuplicateException.class)
    public ResponseEntity<?> handleDocumentDuplicate(DocumentNumberDuplicateException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "DOCUMENT_ALREADY_EXISTS",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Insufficient Balance")
                .message(ex.getMessage())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<?> handleAccountStatus(AccountStatusException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 403,
                        "error", "Forbidden",
                        "message", ex.getMessage()
                ));
    }
}
