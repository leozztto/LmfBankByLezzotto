package com.lezztto.LmfBank.movement.mapper;

import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    Transfer toTransfer(TransferRequest transferRequest);

    @Mapping(target = "transferId", source = "id")
    TransferResponse toTransferResponse(Transfer transfer);
}
