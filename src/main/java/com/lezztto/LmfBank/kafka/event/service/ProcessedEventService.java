package com.lezztto.LmfBank.kafka.event.service;

import com.lezztto.LmfBank.kafka.event.domain.entity.ProcessedEvent;
import com.lezztto.LmfBank.kafka.event.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public void markAsProcessed(String eventId) {

        log.info("Saving event: {}", eventId);

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .processedAt(LocalDateTime.now())
                        .build()
        );
    }

    public boolean alreadyProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }
}
