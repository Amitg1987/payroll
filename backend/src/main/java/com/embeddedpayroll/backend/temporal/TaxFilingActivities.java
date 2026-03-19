package com.embeddedpayroll.backend.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TaxFilingActivities {

    @ActivityMethod
    void generateFiling(
        Long organizationId,
        Integer taxYear,
        String filingPeriod,
        String filingType,
        String requestedBy,
        String workflowId
    );
}
