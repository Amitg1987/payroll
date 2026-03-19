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
    name = "employee_w4_profiles",
    uniqueConstraints = @UniqueConstraint(name = "uk_employee_w4_year", columnNames = {"employee_id", "tax_year"})
)
public class EmployeeW4Profile extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FilingStatus filingStatus;

    @Column(nullable = false)
    private boolean multipleJobs;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal dependentsCredit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal otherIncome = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal extraWithholding = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean exemptFromWithholding;

    public enum FilingStatus {
        SINGLE,
        MARRIED_FILING_JOINTLY,
        HEAD_OF_HOUSEHOLD
    }
}
