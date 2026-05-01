package com.lezztto.LmfBank.account.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
