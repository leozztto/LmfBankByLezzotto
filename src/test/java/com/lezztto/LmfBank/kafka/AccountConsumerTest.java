package com.lezztto.LmfBank.kafka;

import com.lezztto.LmfBank.kafka.dto.AccountEvent;
import com.lezztto.LmfBank.kafka.event.service.AccountEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountConsumerTest {

    @Mock
    private AccountEventService accountEventService;
    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private AccountConsumer accountConsumer;

    private AccountEvent event() {
        AccountEvent e = new AccountEvent();
        e.setEventId("evt-1");
        return e;
    }

    @Test
    @DisplayName("mensagem válida: processa e faz o ack manual")
    void processesAndAcknowledges() {
        AccountEvent event = event();

        accountConsumer.consume(event, ack);

        verify(accountEventService).process(event);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("mensagem nula: ignora, não processa e não faz ack")
    void skipsNullMessage() {
        accountConsumer.consume(null, ack);

        verifyNoInteractions(accountEventService);
        verify(ack, never()).acknowledge();
    }

    @Test
    @DisplayName("falha no processamento: repropaga e não faz ack (mensagem volta para reprocesso)")
    void rethrowsAndDoesNotAckOnFailure() {
        AccountEvent event = event();
        doThrow(new RuntimeException("boom")).when(accountEventService).process(event);

        assertThatThrownBy(() -> accountConsumer.consume(event, ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        verify(ack, never()).acknowledge();
    }
}
