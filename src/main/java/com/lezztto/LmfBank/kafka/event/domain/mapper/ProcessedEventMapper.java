package com.lezztto.LmfBank.kafka.event.domain.mapper;

import com.lezztto.LmfBank.kafka.event.domain.dto.ProcessedEventDto;
import com.lezztto.LmfBank.kafka.event.domain.entity.ProcessedEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProcessedEventMapper {

    ProcessedEvent toProcessedEvent(ProcessedEventDto ProcessedEvent);
}
