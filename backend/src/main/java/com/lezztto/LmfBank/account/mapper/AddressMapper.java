package com.lezztto.LmfBank.account.mapper;

import com.lezztto.LmfBank.account.domain.dto.AddressDto;
import com.lezztto.LmfBank.account.domain.entity.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toAddress(AddressDto dto);
}
