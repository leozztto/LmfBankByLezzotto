package com.lezztto.LmfBank.kafka.mapper;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.kafka.dto.AccountEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountEventMapper {

    AccountDto eventToDto(AccountEvent event);

}
