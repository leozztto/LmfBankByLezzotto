package com.lezztto.LmfBank.kafka;

import com.lezztto.LmfBank.kafka.dto.AccountEvent;
import com.lezztto.LmfBank.kafka.event.service.AccountEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountConsumer {

    private final AccountEventService accountEventService;

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
            accountEventService.process(accountEvent);
            ack.acknowledge();

        } catch (Exception e) {

            log.error("Error processing event: {}", accountEvent.getEventId(), e);
            throw e;
        }
    }
}
