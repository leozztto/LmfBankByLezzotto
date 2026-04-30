package com.lezztto.LmfClients.kafka.event.domain.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventDto {

    private String eventId;

    private LocalDateTime processedAt;
}
