package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_runs")
public class PayrollRun extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "payroll_schedule_id", nullable = false)
    private PayrollSchedule payrollSchedule;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private LocalDate payDate;

    @Column(nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status = RunStatus.DRAFT;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @Column(length = 80)
    private String approvedBy;

    private OffsetDateTime approvedAt;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal deductionTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netTotal = BigDecimal.ZERO;

    public enum RunStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        POSTED,
        REJECTED
    }
}
