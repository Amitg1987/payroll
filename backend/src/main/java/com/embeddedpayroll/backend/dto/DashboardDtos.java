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
        long employeeCount,
        long pendingApprovalRuns,
        long pendingFilings,
        LocalDate nextPayDate,
        List<Integer> supportedTaxYears
    ) {
    }
}
