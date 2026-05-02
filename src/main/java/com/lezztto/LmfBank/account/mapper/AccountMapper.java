package com.lezztto.LmfBank.account.mapper;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        AddressMapper.class,
        AccountBalanceMapper.class
})
public interface AccountMapper {

    Account toAccount(AccountDto accountDto);

    AccountDto toAccountDto(Account account);

    @AfterMapping
    default void mappingRelations(@MappingTarget Account account) {

        if (account.getAddresses() != null) {
            account.getAddresses()
                    .forEach(addr -> addr.setAccount(account));
        }

        if (account.getBalance() != null) {
            account.getBalance()
                    .setAccount(account);
        }
    }
}
