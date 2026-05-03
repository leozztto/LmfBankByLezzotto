package com.lezztto.LmfBank.movement.util;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class AccountValidator {

    public void validateStatusAccountForTransaction(Long accountId, String accountStatus) {

        log.info("Validating status of accounnt: {}", accountId);

        if (!Objects.equals(accountStatus, AccountStatus.ACTIVE.name())) {

            log.info("Transaction is not possible, because the status of accounnt is: {}", accountStatus);

            throw new AccountStatusException(accountId, accountStatus);
        }
    }
}
