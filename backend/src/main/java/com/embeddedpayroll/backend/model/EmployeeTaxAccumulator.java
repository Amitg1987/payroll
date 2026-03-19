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
    name = "employee_tax_accumulators",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_employee_tax_accumulator",
        columnNames = {"employee_id", "tax_year", "tax_code", "jurisdiction_code"}
    )
)
public class EmployeeTaxAccumulator extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_code", nullable = false, length = 48)
    private TaxCode taxCode;

    @Column(name = "jurisdiction_code", nullable = false, length = 32)
    private String jurisdictionCode = "US";

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdTaxableWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdEmployeeTaxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdEmployerTaxAmount = BigDecimal.ZERO;

    public enum TaxCode {
        FEDERAL_INCOME_TAX,
        SOCIAL_SECURITY,
        MEDICARE,
        ADDITIONAL_MEDICARE,
        FUTA,
        STATE_WITHHOLDING,
        LOCAL_WITHHOLDING,
        STATE_UNEMPLOYMENT
    }
}
