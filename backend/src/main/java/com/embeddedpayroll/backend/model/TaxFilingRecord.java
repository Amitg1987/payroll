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
@Table(name = "tax_filing_records")
public class TaxFilingRecord extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FilingType filingType;

    @Column(nullable = false)
    private Integer taxYear;

    @Column(nullable = false, length = 20)
    private String filingPeriod;

    @Column(nullable = false, length = 32)
    private String filingJurisdictionCode = "US";

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RecordStatus status = RecordStatus.GENERATED;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(nullable = false)
    private OffsetDateTime generatedAt = OffsetDateTime.now();

    private OffsetDateTime submittedAt;

    @Column(length = 80)
    private String referenceNumber;

    @Column(length = 120)
    private String temporalWorkflowId;

    @Column(length = 120)
    private String temporalRunId;

    public enum FilingType {
        FORM_941,
        FORM_940,
        FORM_W2,
        FORM_W3,
        STATE_WITHHOLDING,
        LOCAL_WITHHOLDING
    }

    public enum RecordStatus {
        GENERATED,
        SUBMITTED,
        ACCEPTED,
        REJECTED
    }
}
