package com.lezztto.LmfBank.movement.mapper;

import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transfer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    Transfer toTransfer(TransferRequest transferRequest);

    TransferResponse toTransferResponse(Transfer transfer);
}
