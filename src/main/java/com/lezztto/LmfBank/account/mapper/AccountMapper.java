package com.lezztto.LmfBank.account.mapper;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.response.AccountResponse;
import com.lezztto.LmfBank.account.util.MaskUtils;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {
        AddressMapper.class,
        AccountBalanceMapper.class
})
public interface AccountMapper {

    Account toAccount(AccountDto accountDto);

    @Mapping(
            target = "maskedDocument",
            source = "documentNumber",
            qualifiedByName = "maskDocument"
    )
    @Mapping(
            target = "maskedEmail",
            source = "email",
            qualifiedByName = "maskEmail"
    )
    @Mapping(
            target = "maskedPhone",
            source = "phone",
            qualifiedByName = "maskPhone"
    )
    AccountResponse toAccountResponse(Account account);

    @AfterMapping
    default void mappingRelations(@MappingTarget Account account) {

        if (account.getAddresses() != null) {
            account.getAddresses()
                    .forEach(address -> address.setAccount(account));
        }

        if (account.getBalance() != null) {
            account.getBalance().setAccount(account);
        }
    }

    @Named("maskDocument")
    default String maskDocument(String value) {
        return MaskUtils.maskDocument(value);
    }

    @Named("maskEmail")
    default String maskEmail(String value) {
        return MaskUtils.maskEmail(value);
    }

    @Named("maskPhone")
    default String maskPhone(String value) {
        return MaskUtils.maskPhone(value);
    }
}
