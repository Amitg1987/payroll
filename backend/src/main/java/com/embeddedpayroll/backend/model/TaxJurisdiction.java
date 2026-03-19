package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tax_jurisdictions")
public class TaxJurisdiction extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JurisdictionType jurisdictionType;

    @Column(nullable = false, length = 8)
    private String countryCode = "US";

    @Column(length = 8)
    private String stateCode;

    @Column(length = 32)
    private String parentJurisdictionCode;

    @Column(nullable = false)
    private boolean active = true;

    public enum JurisdictionType {
        FEDERAL,
        STATE,
        LOCAL
    }
}
