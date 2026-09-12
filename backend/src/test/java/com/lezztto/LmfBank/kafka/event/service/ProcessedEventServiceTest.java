package com.lezztto.LmfBank.kafka.event.service;

import com.lezztto.LmfBank.kafka.event.domain.entity.ProcessedEvent;
import com.lezztto.LmfBank.kafka.event.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ProcessedEventService processedEventService;

    @Test
    @DisplayName("markAsProcessed grava o eventId com timestamp de processamento")
    void marksEventAsProcessed() {
        processedEventService.markAsProcessed("evt-42");

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("evt-42");
        assertThat(captor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("alreadyProcessed delega para existsById")
    void checksAlreadyProcessed() {
        when(processedEventRepository.existsById("known")).thenReturn(true);
        when(processedEventRepository.existsById("unknown")).thenReturn(false);

        assertThat(processedEventService.alreadyProcessed("known")).isTrue();
        assertThat(processedEventService.alreadyProcessed("unknown")).isFalse();
    }
}
