package com.embeddedpayroll.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record PayrollSummaryReportResponse(
        Long organizationId,
        Integer taxYear,
        BigDecimal grossTotal,
        BigDecimal federalTaxTotal,
        BigDecimal stateTaxTotal,
        BigDecimal localTaxTotal,
        BigDecimal employerTaxTotal,
        BigDecimal netTotal,
        List<JurisdictionSummary> stateSummaries
    ) {
    }

    public record JurisdictionSummary(
        String jurisdictionCode,
        BigDecimal grossTotal,
        BigDecimal employeeTaxTotal,
        BigDecimal employerTaxTotal,
        BigDecimal netTotal
    ) {
    }
}
