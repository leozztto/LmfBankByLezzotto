package com.lezztto.LmfBank.account.domain.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {

    private BigDecimal availableBalance;

    private BigDecimal blockedBalance;

    private BigDecimal totalBalance;
}
