package com.lezztto.LmfBank.exception.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Standard API error response")
@Data
@Builder
public class ApiError {

    @Schema(example = "400")
    private Integer status;

    @Schema(example = "INSUFFICIENT_BALANCE")
    private String code;

    @Schema(example = "Insufficient balance for transaction")
    private String message;

    @Schema(example = "/transfers")
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
