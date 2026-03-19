package com.embeddedpayroll.backend.dto;

import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSummaryResponse(
        Long organizationId,
        String organizationName,
        String ein,
        String primaryJurisdictionCode,
        long employeeCount,
        long pendingApprovalRuns,
        long pendingFilings,
        long jurisdictionRegistrations,
        long partnerApiClients,
        long pendingWebhookDeliveries,
        LocalDate nextPayDate,
        List<Integer> supportedTaxYears
    ) {
    }
}
