package com.lezztto.LmfBank.account.mapper;

import com.lezztto.LmfBank.account.domain.dto.AccountBalanceDto;
import com.lezztto.LmfBank.account.domain.entity.AccountBalance;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountBalanceMapper {

    AccountBalance toAccountBalance(AccountBalanceDto dto);
}
