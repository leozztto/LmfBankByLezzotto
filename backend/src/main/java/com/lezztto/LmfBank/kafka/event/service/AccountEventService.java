package com.lezztto.LmfBank.kafka.event.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.kafka.dto.AccountEvent;
import com.lezztto.LmfBank.kafka.mapper.AccountEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventService {

    private final AccountService accountService;
    private final AccountEventMapper accountEventMapper;
    private final ProcessedEventService processedEventService;

    @Transactional
    public void process(AccountEvent accountEvent) {

        log.info("Processing event: {}", accountEvent.getEventId());

        if (processedEventService.alreadyProcessed(accountEvent.getEventId())) {
            return;
        }

        var accountDto = accountEventMapper.eventToDto(accountEvent);

        accountService.create(accountDto);

        processedEventService.markAsProcessed(accountEvent.getEventId());
    }
}
