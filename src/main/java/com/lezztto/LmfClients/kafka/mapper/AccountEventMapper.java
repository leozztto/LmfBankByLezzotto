package com.lezztto.LmfClients.kafka.mapper;

import com.lezztto.LmfClients.account.domain.dto.AccountDto;
import com.lezztto.LmfClients.kafka.dto.AccountEvent;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountEventMapper {

    AccountDto eventToDto(AccountEvent event);

    @AfterMapping
    default void mappingForAddress(@MappingTarget AccountDto accountDto) {
        if (accountDto.getAddresses() != null) {
            accountDto.getAddresses().forEach(addr -> addr.setAccount(accountDto));
        }
    }
}
