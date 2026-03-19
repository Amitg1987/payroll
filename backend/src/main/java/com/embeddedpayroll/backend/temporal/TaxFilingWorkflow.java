package com.embeddedpayroll.backend.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface TaxFilingWorkflow {

    @WorkflowMethod
    TaxFilingWorkflowResult run(TaxFilingWorkflowRequestPayload requestPayload);

    record TaxFilingWorkflowRequestPayload(
        Long organizationId,
        Integer taxYear,
        String filingPeriod,
        List<String> filingTypes,
        String requestedBy
    ) {
    }

    record TaxFilingWorkflowResult(String workflowMessage) {
    }
}
