package com.lezztto.LmfClients.kafka.event.repository;

import com.lezztto.LmfClients.kafka.event.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
