package com.lezztto.LmfClients.kafka.dto;

import com.lezztto.LmfClients.account.domain.entity.AccountType;
import com.lezztto.LmfClients.account.domain.entity.Nationality;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AccountEvent {

    private String eventId;

    private String fullName;

    private String documentNumber;

    private LocalDateTime birthDate;

    private String motherName;

    private Nationality nationality;

    private String email;

    private String phone;

    private List<AddressEvent> addresses;

    private String profession;

    private BigDecimal monthlyIncome;

    private AccountType accountType;

    private String agency;

    private Boolean acceptedTerms;

    private LocalDateTime createdAt;
}
