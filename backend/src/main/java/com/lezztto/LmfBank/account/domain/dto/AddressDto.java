package com.lezztto.LmfBank.account.domain.dto;

import com.lezztto.LmfBank.account.domain.enums.AddressType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private String zipCode;

    private String street;

    private String neighborhood;

    private String number;

    private String complement;

    private String city;

    private String state;

    private String country;

    private AddressType addressType;
}
