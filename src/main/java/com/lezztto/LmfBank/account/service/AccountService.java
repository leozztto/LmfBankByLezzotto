package com.lezztto.LmfBank.account.service;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.entity.AccountBalance;
import com.lezztto.LmfBank.account.domain.response.AccountResponse;
import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.exception.DocumentNumberDuplicateException;
import com.lezztto.LmfBank.account.mapper.AccountMapper;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import com.lezztto.LmfBank.account.util.AccountNumberGenerator;
import jakarta.transaction.Transactional;
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

    @Transactional
    public AccountResponse create(AccountDto accountDto) throws DocumentNumberDuplicateException {

        log.info("Initializing new account registration");

        validateDocumentNumber(accountDto.getDocumentNumber());

        accountDto.setAccountNumber(AccountNumberGenerator.generateAccountNumber());

        log.info("mapping account data to the document : {}", accountDto.getDocumentNumber());

        var accountEntity = accountMapper.toAccount(accountDto);

        setAddressesForAccount(accountEntity);

        initializeBalances(accountEntity);

        var account = accountRepository.save(accountEntity);

        log.info("Account: {} generated for documentNumber: {}", account.getAccountNumber(), account.getDocumentNumber());

        return accountMapper.toAccountResponse(account);
    }

    @Transactional
    public Account save(Account account) {

        log.info("Saving account: {}", account.getId());

        return accountRepository.save(account);
    }

    private void validateDocumentNumber(String documentNumber) {
        if (accountRepository.existsByDocumentNumber(documentNumber)) {
            log.warn("Document already registered: {}", documentNumber);

            throw new DocumentNumberDuplicateException(documentNumber);
        }
    }

    private void setAddressesForAccount(Account accountEntity){
        if (accountEntity.getAddresses() != null) {
            accountEntity.getAddresses()
                    .forEach(a -> a.setAccount(accountEntity));
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

    public AccountResponse findById(Long accountId) {

        log.info("Finding account by id: {}", accountId);

        var account =  accountRepository.findByIdWithRelations(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return accountMapper.toAccountResponse(account);
    }

    public Account findByIdAccount(Long accountId){

        return accountRepository.findByIdWithRelations(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public AccountResponse findByDocumentNumber(String documentNumber) {

        log.info("Finding account by document number: {}", documentNumber);

        var account = accountRepository.findByDocumentNumberWithRelations(documentNumber)
                .orElseThrow(() -> new AccountNotFoundException(documentNumber));

        return accountMapper.toAccountResponse(account);
    }
}
