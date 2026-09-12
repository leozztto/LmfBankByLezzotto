package com.lezztto.LmfBank.movement.idempotency;

import com.lezztto.LmfBank.movement.domain.entity.Transfer;
import com.lezztto.LmfBank.movement.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferIdempotencyService {

    private final TransferRepository transferRepository;

    public boolean tryLock(UUID key) {

        return !transferRepository.existsByIdempotencyKey(key);
    }

    public Transfer findByKey(UUID key) {
        return transferRepository.findByIdempotencyKey(key).orElseThrow();
    }
}
