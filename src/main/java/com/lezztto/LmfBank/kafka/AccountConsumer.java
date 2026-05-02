package com.lezztto.LmfBank.kafka;

import com.lezztto.LmfBank.kafka.event.domain.dto.ProcessedEventDto;
import com.lezztto.LmfBank.kafka.dto.AccountEvent;
import com.lezztto.LmfBank.kafka.mapper.AccountEventMapper;
import com.lezztto.LmfBank.kafka.event.service.ProcessedEventService;
import com.lezztto.LmfBank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountConsumer {

    private final AccountService accountService;

    private final AccountEventMapper accountEventMapper;

    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "account-create",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(AccountEvent accountEvent, Acknowledgment ack) {

        if (accountEvent == null) {
            log.warn("Received null or invalid Kafka message, skipping");
            return;
        }

        log.info("Received event: {}", accountEvent.getEventId());

        try {
            var processedEventDto = new ProcessedEventDto(accountEvent.getEventId(), LocalDateTime.now());
            if (!processedEventService.tryProcess(processedEventDto)) {
                return;
            }

            var account = accountEventMapper.eventToDto(accountEvent);
            accountService.create(account);

            processedEventService.markAsProcessed(processedEventDto);

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing event: {}", accountEvent, e);
            throw e;
        }
    }
}
