package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tax_filing_workflow_requests")
public class TaxFilingWorkflowRequest extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private Integer taxYear;

    @Column(nullable = false, length = 20)
    private String filingPeriod;

    @Column(nullable = false, length = 240)
    private String requestedFilings;

    @Column(nullable = false, length = 80)
    private String requestedBy;

    @Column(nullable = false, length = 120)
    private String workflowId;

    @Column(length = 120)
    private String workflowRunId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WorkflowStatus workflowStatus = WorkflowStatus.QUEUED;

    @Column(nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    @Column(length = 240)
    private String workflowMessage;

    public enum WorkflowStatus {
        QUEUED,
        STARTED,
        COMPLETED,
        FAILED,
        LOCAL_DEV_FALLBACK
    }
}
