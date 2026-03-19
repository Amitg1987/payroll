package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "jurisdiction_tax_profiles",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_jurisdiction_tax_profile",
        columnNames = {"tax_jurisdiction_id", "tax_year", "tax_type"}
    )
)
public class JurisdictionTaxProfile extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "tax_jurisdiction_id", nullable = false)
    private TaxJurisdiction taxJurisdiction;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 40)
    private TaxType taxType;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal residentRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal nonResidentRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal employerRate = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal wageBase;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal standardDeduction = BigDecimal.ZERO;

    @Column(length = 240)
    private String notes;

    public enum TaxType {
        STATE_WITHHOLDING,
        LOCAL_WITHHOLDING,
        STATE_UNEMPLOYMENT
    }
}
