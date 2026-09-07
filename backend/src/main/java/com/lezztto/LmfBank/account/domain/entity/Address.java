package com.lezztto.LmfBank.account.domain.entity;

import com.lezztto.LmfBank.account.domain.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private String zipCode;

    private String street;

    private String neighborhood;

    private String number;

    private String complement;

    private String city;

    private String state;

    private String country;

    @Enumerated(EnumType.STRING)
    private AddressType addressType;
}
