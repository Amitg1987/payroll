package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_run_item_allocations")
public class PayrollRunItemAllocation extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "payroll_run_item_id", nullable = false)
    private PayrollRunItem payrollRunItem;

    @Column(nullable = false, length = 32)
    private String stateJurisdictionCode;

    @Column(length = 32)
    private String localJurisdictionCode;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal allocationPercentage = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedGrossWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedTaxableWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal workStateIncomeTax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal localIncomeTax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal stateUnemploymentTaxableWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal employerStateUnemploymentTax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal residentStateCreditApplied = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean reciprocityApplied;
}
