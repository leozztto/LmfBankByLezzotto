package com.lezztto.LmfBank.account.domain.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceDto {

    private AccountDto account;

    private BigDecimal availableBalance;

    private BigDecimal blockedBalance;

    private BigDecimal totalBalance;
}
