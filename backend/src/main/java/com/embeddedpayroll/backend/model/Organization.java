package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String legalName;

    @Column(length = 160)
    private String dbaName;

    @Column(nullable = false, unique = true, length = 32)
    private String ein;

    @Column(nullable = false, length = 8)
    private String defaultStateCode;

    @Column(nullable = false, length = 8)
    private String defaultCurrency = "USD";

    @Column(nullable = false, length = 40)
    private String accountingMethod = "ACCRUAL";

    @Column(nullable = false, length = 120)
    private String contactEmail;
}
