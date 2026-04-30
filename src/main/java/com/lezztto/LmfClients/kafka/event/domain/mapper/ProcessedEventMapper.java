package com.lezztto.LmfClients.kafka.event.domain.mapper;

import com.lezztto.LmfClients.kafka.event.domain.dto.ProcessedEventDto;
import com.lezztto.LmfClients.kafka.event.domain.entity.ProcessedEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProcessedEventMapper {

    ProcessedEvent toProcessedEvent(ProcessedEventDto ProcessedEvent);
}
