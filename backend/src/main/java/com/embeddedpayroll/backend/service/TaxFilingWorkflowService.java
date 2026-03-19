package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.config.TemporalProperties;
import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.model.TaxFilingWorkflowRequest;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.TaxFilingWorkflowRequestRepository;
import com.embeddedpayroll.backend.temporal.TaxFilingWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxFilingWorkflowService {

    private final TaxFilingWorkflowRequestRepository workflowRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final TaxService taxService;
    private final WebhookService webhookService;
    private final TemporalProperties temporalProperties;
    private final ObjectProvider<WorkflowClient> workflowClientProvider;

    @Transactional(readOnly = true)
    public List<TaxFilingWorkflowRequest> listRequests(Long organizationId) {
        return workflowRequestRepository.findByOrganizationIdOrderByRequestedAtDesc(organizationId);
    }

    @Transactional
    public TaxFilingWorkflowRequest startWorkflow(
        TaxDtos.StartFilingWorkflowRequest request,
        String requestedBy
    ) {
        Organization organization = organizationRepository.findById(request.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));

        TaxFilingWorkflowRequest workflowRequest = new TaxFilingWorkflowRequest();
        workflowRequest.setOrganization(organization);
        workflowRequest.setTaxYear(request.taxYear());
        workflowRequest.setFilingPeriod(request.filingPeriod().toUpperCase());
        workflowRequest.setRequestedFilings(request.filingTypes().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")));
        workflowRequest.setRequestedBy(requestedBy);
        workflowRequest.setRequestedAt(OffsetDateTime.now());
        workflowRequest.setWorkflowId("tax-filing-" + organization.getTenantKey() + "-" + UUID.randomUUID());

        WorkflowClient workflowClient = temporalProperties.enabled() ? workflowClientProvider.getIfAvailable() : null;
        if (workflowClient != null) {
            TaxFilingWorkflow workflow = workflowClient.newWorkflowStub(
                TaxFilingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(temporalProperties.taskQueue())
                    .setWorkflowId(workflowRequest.getWorkflowId())
                    .build()
            );
            WorkflowExecution workflowExecution = WorkflowClient.start(
                workflow::run,
                new TaxFilingWorkflow.TaxFilingWorkflowRequestPayload(
                    request.organizationId(),
                    request.taxYear(),
                    request.filingPeriod(),
                    request.filingTypes().stream().map(Enum::name).toList(),
                    requestedBy
                )
            );
            workflowRequest.setWorkflowRunId(workflowExecution.getRunId());
            workflowRequest.setWorkflowStatus(TaxFilingWorkflowRequest.WorkflowStatus.STARTED);
            workflowRequest.setWorkflowMessage("Temporal workflow started");
        } else {
            for (TaxFilingRecord.FilingType filingType : request.filingTypes()) {
                taxService.generateFiling(
                    new TaxDtos.GenerateFilingRequest(
                        request.organizationId(),
                        request.taxYear(),
                        filingType,
                        request.filingPeriod(),
                        null,
                        null
                    ),
                    requestedBy,
                    workflowRequest.getWorkflowId(),
                    null
                );
            }
            workflowRequest.setWorkflowStatus(TaxFilingWorkflowRequest.WorkflowStatus.LOCAL_DEV_FALLBACK);
            workflowRequest.setWorkflowMessage("Temporal disabled; filings generated via local fallback");
        }

        TaxFilingWorkflowRequest savedRequest = workflowRequestRepository.save(workflowRequest);
        webhookService.enqueueEvent(
            request.organizationId(),
            "tax.filing.workflow.started",
            savedRequest.getWorkflowId(),
            TaxDtos.fromEntity(savedRequest)
        );
        return savedRequest;
    }
}
