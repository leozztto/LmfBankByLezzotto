package com.lezztto.LmfBank.kafka.event.repository;

import com.lezztto.LmfBank.kafka.event.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
