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

        validateDocumentNumber(accountDto.getDocumentNumber());

        accountDto.setAccountNumber(AccountNumberGenerator.generateAccountNumber());

        log.info("Saving account : {}", accountDto.getAccountNumber());

        var accountEntity = accountMapper.toAccount(accountDto);

        initializeBalances(accountEntity);

        var account = accountRepository.save(accountEntity);

        return accountMapper.toAccountDto(account);
    }

    private void validateDocumentNumber(String documentNumber) {
        if (accountRepository.existsByDocumentNumber(documentNumber)) {
            log.warn("Document already registered: {}", documentNumber);

            throw new DocumentNumberDuplicateException(documentNumber);
        }
    }

    private void initializeBalances(Account account) {

        log.info("Generating balances");

        var balances = AccountBalance.builder()
                .availableBalance(BigDecimal.ZERO)
                .blockedBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        account.addBalance(balances);
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
