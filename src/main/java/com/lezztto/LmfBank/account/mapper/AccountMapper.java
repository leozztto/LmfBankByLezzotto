package com.lezztto.LmfBank.account.mapper;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toAccount(AccountDto accountDto);

    AccountDto toAccountDto(Account account);

    @AfterMapping
    default void mappingForAddress(@MappingTarget Account account) {
        if (account.getAddresses() != null) {
            account.getAddresses().forEach(addr -> addr.setAccount(account));
        }
    }
}
