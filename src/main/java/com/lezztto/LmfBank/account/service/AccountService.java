package com.lezztto.LmfBank.account.service;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.entity.AccountBalance;
import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.exception.DocumentNumberDuplicateException;
import com.lezztto.LmfBank.account.mapper.AccountMapper;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import com.lezztto.LmfBank.account.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountMapper accountMapper;

    public AccountDto create(AccountDto accountDto) throws DocumentNumberDuplicateException {

        var accountEntity = accountMapper.toAccount(accountDto);

        validateDocumentNumber(accountEntity);

        accountEntity.getAddresses().forEach(address -> address.setAccount(accountEntity));

        accountEntity.setAccountNumber(AccountNumberGenerator.generateAccountNumber());

        initializeBalance(accountEntity);

        log.info("Saving account : {}", accountEntity.getAccountNumber());

        var account = accountRepository.save(accountEntity);

        return accountMapper.toAccountDto(account);
    }

    private void validateDocumentNumber(Account account) {
        if (accountRepository.existsByDocumentNumber(account.getDocumentNumber())) {
            log.warn("Document already registered: {}", account.getDocumentNumber());

            throw new DocumentNumberDuplicateException(account.getDocumentNumber());
        }
    }

    private void initializeBalance(Account account) {

        log.info("Generating balances");

        var balance = AccountBalance.builder()
                .availableBalance(BigDecimal.ZERO)
                .blockedBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        account.addBalance(balance);
    }

    public AccountDto findById(Long accountId) {

        log.info("Finding account by id: {}", accountId);

        var account =  accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));


        return accountMapper.toAccountDto(account);
    }

    public AccountDto findByDocumentNumber(String documentNumber) {

        log.info("Finding account by document number: {}", documentNumber);

        var account = accountRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new AccountNotFoundException(documentNumber));

        return accountMapper.toAccountDto(account);
    }

}
