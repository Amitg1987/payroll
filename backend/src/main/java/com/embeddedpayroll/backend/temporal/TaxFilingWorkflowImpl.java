package com.embeddedpayroll.backend.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TaxFilingWorkflowImpl implements TaxFilingWorkflow {

    private final TaxFilingActivities taxFilingActivities = Workflow.newActivityStub(
        TaxFilingActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(2)).build()
    );

    @Override
    public TaxFilingWorkflowResult run(TaxFilingWorkflowRequestPayload requestPayload) {
        for (String filingType : requestPayload.filingTypes()) {
            taxFilingActivities.generateFiling(
                requestPayload.organizationId(),
                requestPayload.taxYear(),
                requestPayload.filingPeriod(),
                filingType,
                requestPayload.requestedBy(),
                Workflow.getInfo().getWorkflowId()
            );
        }
        return new TaxFilingWorkflowResult(
            "Generated " + requestPayload.filingTypes().size() + " filing record(s) through Temporal orchestration"
        );
    }
}
