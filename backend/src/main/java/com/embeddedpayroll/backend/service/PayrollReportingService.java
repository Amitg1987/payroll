package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.ReportDtos;
import com.embeddedpayroll.backend.model.PayrollRunItem;
import com.embeddedpayroll.backend.model.PayrollRunItemAllocation;
import com.embeddedpayroll.backend.repository.PayrollRunItemAllocationRepository;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollReportingService {

    private final PayrollRunItemRepository payrollRunItemRepository;
    private final PayrollRunItemAllocationRepository payrollRunItemAllocationRepository;

    @Transactional(readOnly = true)
    public ReportDtos.PayrollSummaryReportResponse payrollSummary(Long organizationId, Integer taxYear) {
        List<PayrollRunItem> items = payrollRunItemRepository.findByPayrollRunPayrollScheduleOrganizationIdAndTaxYear(
            organizationId,
            taxYear
        );

        BigDecimal grossTotal = sum(items, PayrollRunItem::getGrossPay);
        BigDecimal federalTaxTotal = items.stream()
            .map(item -> item.getFederalIncomeTax()
                .add(item.getSocialSecurityEmployeeTax())
                .add(item.getMedicareEmployeeTax())
                .add(item.getAdditionalMedicareEmployeeTax()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stateTaxTotal = sum(items, PayrollRunItem::getStateIncomeTax);
        BigDecimal localTaxTotal = sum(items, PayrollRunItem::getLocalIncomeTax);
        BigDecimal employerTaxTotal = items.stream()
            .map(item -> item.getEmployerSocialSecurityTax()
                .add(item.getEmployerMedicareTax())
                .add(item.getEmployerFutaTax())
                .add(item.getEmployerStateUnemploymentTax()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netTotal = sum(items, PayrollRunItem::getNetPay);

        List<PayrollRunItemAllocation> allocations = items.stream()
            .flatMap(item -> payrollRunItemAllocationRepository
                .findByPayrollRunItemIdOrderByStateJurisdictionCodeAscLocalJurisdictionCodeAsc(item.getId())
                .stream())
            .toList();

        java.util.Map<String, MutableJurisdictionSummary> stateSummaryMap = new java.util.LinkedHashMap<>();

        for (PayrollRunItem item : items) {
            if (item.getResidentStateJurisdictionCode() != null && item.getResidentStateIncomeTax().signum() > 0) {
                MutableJurisdictionSummary summary = stateSummaryMap.computeIfAbsent(
                    item.getResidentStateJurisdictionCode(),
                    ignored -> new MutableJurisdictionSummary()
                );
                summary.grossTotal = summary.grossTotal.add(item.getGrossPay());
                summary.employeeTaxTotal = summary.employeeTaxTotal.add(item.getResidentStateIncomeTax());
                summary.netTotal = summary.netTotal.add(item.getNetPay());
            }
        }

        for (PayrollRunItemAllocation allocation : allocations) {
            MutableJurisdictionSummary summary = stateSummaryMap.computeIfAbsent(
                allocation.getStateJurisdictionCode(),
                ignored -> new MutableJurisdictionSummary()
            );
            summary.grossTotal = summary.grossTotal.add(allocation.getAllocatedGrossWages());
            summary.employeeTaxTotal = summary.employeeTaxTotal
                .add(allocation.getWorkStateIncomeTax())
                .add(allocation.getLocalIncomeTax());
            summary.employerTaxTotal = summary.employerTaxTotal.add(allocation.getEmployerStateUnemploymentTax());
        }

        List<ReportDtos.JurisdictionSummary> stateSummaries = stateSummaryMap.entrySet()
            .stream()
            .map(entry -> new ReportDtos.JurisdictionSummary(
                entry.getKey(),
                entry.getValue().grossTotal,
                entry.getValue().employeeTaxTotal,
                entry.getValue().employerTaxTotal,
                entry.getValue().netTotal
            ))
            .sorted(Comparator.comparing(ReportDtos.JurisdictionSummary::jurisdictionCode))
            .toList();

        return new ReportDtos.PayrollSummaryReportResponse(
            organizationId,
            taxYear,
            grossTotal,
            federalTaxTotal,
            stateTaxTotal,
            localTaxTotal,
            employerTaxTotal,
            netTotal,
            stateSummaries
        );
    }

    private BigDecimal sum(List<PayrollRunItem> items, java.util.function.Function<PayrollRunItem, BigDecimal> mapper) {
        return items.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static final class MutableJurisdictionSummary {
        private BigDecimal grossTotal = BigDecimal.ZERO;
        private BigDecimal employeeTaxTotal = BigDecimal.ZERO;
        private BigDecimal employerTaxTotal = BigDecimal.ZERO;
        private BigDecimal netTotal = BigDecimal.ZERO;
    }
}
