package com.lezztto.LmfBank.movement.util;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AccountValidator {

    public void validateForTransaction(Long accountId, String accountStatus) {

        if (!Objects.equals(accountStatus, AccountStatus.ACTIVE.name())) {

            throw new AccountStatusException(accountId, accountStatus);
        }
    }
}
