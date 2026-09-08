package com.lezztto.LmfBank.account.domain.dto;

import com.lezztto.LmfBank.account.domain.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    @NotBlank
    private String zipCode;

    @NotBlank
    private String street;

    @NotBlank
    private String neighborhood;

    @NotBlank
    private String number;

    private String complement;

    @NotBlank
    private String city;

    @NotBlank
    @Size(min = 2, max = 2, message = "state deve ter 2 caracteres")
    private String state;

    private String country;

    @NotNull
    private AddressType addressType;
}
