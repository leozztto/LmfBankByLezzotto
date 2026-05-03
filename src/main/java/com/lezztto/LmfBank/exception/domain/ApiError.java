package com.lezztto.LmfBank.exception.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiError {
    private int status;
    private String code;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
