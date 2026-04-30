package com.lezztto.LmfClients.account.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lezztto.LmfClients.account.domain.entity.AccountType;
import com.lezztto.LmfClients.account.domain.entity.Nationality;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private String eventId;

    private String fullName;

    private String documentNumber;

    private LocalDateTime birthDate;

    private String motherName;

    private Nationality nationality;

    private String email;

    private String phone;

    private List<AddressDto> addresses;

    private String profession;

    private BigDecimal monthlyIncome;

    private AccountType accountType;

    private String accountNumber;

    private String agency;

    private Boolean acceptedTerms;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
