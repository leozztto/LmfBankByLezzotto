package com.lezztto.LmfBank.account.service;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.exception.DocumentNumberDuplicateException;
import com.lezztto.LmfBank.account.mapper.AccountMapper;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import com.lezztto.LmfBank.account.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountMapper accountMapper;

    public Account create(AccountDto accountDto) throws DocumentNumberDuplicateException {

        var account = accountMapper.toAccount(accountDto);

        if(accountRepository.existsByDocumentNumber(account.getDocumentNumber())) {
            log.warn("Document already registered: {}", account.getDocumentNumber());

            throw new DocumentNumberDuplicateException(account.getDocumentNumber());
        }

        account.getAddresses().forEach(address -> address.setAccount(account));

        account.setAccountNumber(AccountNumberGenerator.generateAccountNumber());

        log.info("Saving account : {}", account.getAccountNumber());

        return accountRepository.save(account);
    }

}
