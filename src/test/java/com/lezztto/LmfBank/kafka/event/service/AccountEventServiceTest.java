package com.lezztto.LmfBank.kafka.event.service;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.kafka.dto.AccountEvent;
import com.lezztto.LmfBank.kafka.mapper.AccountEventMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Consumo idempotente de eventos de criação de conta.
 */
@ExtendWith(MockitoExtension.class)
class AccountEventServiceTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountEventMapper accountEventMapper;
    @Mock
    private ProcessedEventService processedEventService;

    @InjectMocks
    private AccountEventService accountEventService;

    private AccountEvent event(String id) {
        AccountEvent e = new AccountEvent();
        e.setEventId(id);
        return e;
    }

    @Test
    @DisplayName("evento novo: mapeia, cria a conta e marca como processado")
    void processesNewEvent() {
        AccountEvent event = event("evt-1");
        AccountDto dto = AccountDto.builder().documentNumber("12345678901").build();
        when(processedEventService.alreadyProcessed("evt-1")).thenReturn(false);
        when(accountEventMapper.eventToDto(event)).thenReturn(dto);

        accountEventService.process(event);

        verify(accountService).create(dto);
        verify(processedEventService).markAsProcessed("evt-1");
    }

    @Test
    @DisplayName("evento já processado: sai sem criar conta nem remarcar")
    void skipsAlreadyProcessedEvent() {
        AccountEvent event = event("evt-dup");
        when(processedEventService.alreadyProcessed("evt-dup")).thenReturn(true);

        accountEventService.process(event);

        verifyNoInteractions(accountEventMapper);
        verifyNoInteractions(accountService);
        verify(processedEventService).alreadyProcessed("evt-dup");
    }
}
