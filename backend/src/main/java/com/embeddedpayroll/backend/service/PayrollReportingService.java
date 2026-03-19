package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.ReportDtos;
import com.embeddedpayroll.backend.model.PayrollRunItem;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollReportingService {

    private final PayrollRunItemRepository payrollRunItemRepository;

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

        List<ReportDtos.JurisdictionSummary> stateSummaries = items.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                item -> item.getStateJurisdictionCode() == null ? "UNSPECIFIED" : item.getStateJurisdictionCode()
            ))
            .entrySet()
            .stream()
            .map(entry -> {
                List<PayrollRunItem> groupItems = entry.getValue();
                return new ReportDtos.JurisdictionSummary(
                    entry.getKey(),
                    sum(groupItems, PayrollRunItem::getGrossPay),
                    sum(groupItems, PayrollRunItem::getEmployeeTaxTotal),
                    groupItems.stream()
                        .map(item -> item.getEmployerSocialSecurityTax()
                            .add(item.getEmployerMedicareTax())
                            .add(item.getEmployerFutaTax())
                            .add(item.getEmployerStateUnemploymentTax()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                    sum(groupItems, PayrollRunItem::getNetPay)
                );
            })
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
}
