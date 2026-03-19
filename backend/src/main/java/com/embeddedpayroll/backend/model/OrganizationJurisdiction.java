package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_jurisdictions")
public class OrganizationJurisdiction extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tax_jurisdiction_id", nullable = false)
    private TaxJurisdiction taxJurisdiction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NexusType nexusType;

    @Column(length = 80)
    private String registrationNumber;

    @Column(length = 80)
    private String employerAccountNumber;

    @Column(nullable = false)
    private boolean primaryJurisdiction;

    public enum NexusType {
        REGISTERED,
        WORK_LOCATION,
        RESIDENCE,
        BOTH
    }
}
