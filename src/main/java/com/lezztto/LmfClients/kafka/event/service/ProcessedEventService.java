package com.lezztto.LmfClients.kafka.event.service;

import com.lezztto.LmfClients.kafka.event.domain.dto.ProcessedEventDto;
import com.lezztto.LmfClients.kafka.event.domain.entity.ProcessedEvent;
import com.lezztto.LmfClients.kafka.event.domain.mapper.ProcessedEventMapper;
import com.lezztto.LmfClients.kafka.event.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    private final ProcessedEventMapper processedEventMapper;

    public void markAsProcessed(ProcessedEventDto processedEventDto) {

        var processedEvent = processedEventMapper.toProcessedEvent(processedEventDto);

        log.info("Saving event: {}", processedEvent.getEventId());

        processedEventRepository.save(processedEvent);

    }

    public boolean tryProcess(ProcessedEventDto processedEventDto) {
        try {
            processedEventRepository.save(
                    new ProcessedEvent(processedEventDto.getEventId(), LocalDateTime.now())
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
