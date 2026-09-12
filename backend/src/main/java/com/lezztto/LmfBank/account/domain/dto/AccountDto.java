package com.lezztto.LmfBank.account.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.domain.enums.AccountType;
import com.lezztto.LmfBank.account.domain.enums.Nationality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private String eventId;

    @NotBlank
    private String fullName;

    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "documentNumber deve ter 11 dígitos")
    private String documentNumber;

    @NotNull
    @Past
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotBlank
    private String motherName;

    @NotNull
    private Nationality nationality;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    @NotEmpty
    @Valid
    private List<AddressDto> addresses;

    @NotBlank
    private String profession;

    @NotNull
    @PositiveOrZero
    private BigDecimal monthlyIncome;

    @NotNull
    private AccountType accountType;

    private String accountNumber;

    private String agency;

    @NotNull
    @AssertTrue(message = "acceptedTerms deve ser true")
    private Boolean acceptedTerms;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private AccountStatus accountStatus;

    private AccountBalanceDto balance;
}
