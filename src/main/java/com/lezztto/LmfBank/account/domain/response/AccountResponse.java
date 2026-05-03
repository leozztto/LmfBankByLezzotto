package com.lezztto.LmfBank.account.domain.response;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.domain.enums.AccountType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {

    private Long id;

    private String fullName;

    private String maskedDocument;

    private String maskedEmail;

    private String maskedPhone;

    private AccountType accountType;

    private String accountNumber;

    private String agency;

    private AccountStatus accountStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private AccountBalanceResponse balance;

    private List<AddressResponse> addresses;
}
