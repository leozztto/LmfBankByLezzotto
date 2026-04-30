package com.lezztto.LmfClients.account.mapper;

import com.lezztto.LmfClients.account.domain.dto.AccountDto;
import com.lezztto.LmfClients.account.domain.entity.Account;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toAccount(AccountDto accountDto);

    @AfterMapping
    default void mappingForAddress(@MappingTarget Account account) {
        if (account.getAddresses() != null) {
            account.getAddresses().forEach(addr -> addr.setAccount(account));
        }
    }
}
