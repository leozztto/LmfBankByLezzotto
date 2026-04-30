package com.lezztto.LmfClients.account.service;

import com.lezztto.LmfClients.account.domain.dto.AccountDto;
import com.lezztto.LmfClients.account.domain.entity.Account;
import com.lezztto.LmfClients.account.mapper.AccountMapper;
import com.lezztto.LmfClients.account.repository.AccountRepository;
import com.lezztto.LmfClients.account.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountMapper accountMapper;

    public Account create(AccountDto accountDto) {

        var account = accountMapper.toAccount(accountDto);

        if(accountRepository.existsByDocumentNumber(account.getDocumentNumber())) {
            return null;
        }

        account.getAddresses().forEach(address -> address.setAccount(account));

        account.setAccountNumber(AccountNumberGenerator.generateAccountNumber());

        log.info("Saving account : {}", account.getAccountNumber());

        return accountRepository.save(account);
    }

}
