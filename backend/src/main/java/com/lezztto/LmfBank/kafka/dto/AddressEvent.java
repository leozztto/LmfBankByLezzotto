package com.lezztto.LmfBank.kafka.dto;

import com.lezztto.LmfBank.account.domain.enums.AddressType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressEvent {

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
