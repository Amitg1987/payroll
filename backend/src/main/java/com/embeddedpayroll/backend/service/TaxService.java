package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.PayrollRun;
import com.embeddedpayroll.backend.model.PayrollRunItem;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import com.embeddedpayroll.backend.repository.PayrollRunRepository;
import com.embeddedpayroll.backend.repository.TaxFilingRecordRepository;
import com.embeddedpayroll.backend.repository.TaxYearProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxYearProfileRepository taxYearProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunItemRepository payrollRunItemRepository;
    private final TaxFilingRecordRepository taxFilingRecordRepository;

    @Transactional(readOnly = true)
    public List<TaxYearProfile> listTaxYears() {
        return taxYearProfileRepository.findAllByOrderByTaxYearDesc();
    }

    @Transactional(readOnly = true)
    public List<TaxFilingRecord> listFilings(Long organizationId, Integer taxYear) {
        return taxFilingRecordRepository.findByOrganizationIdAndTaxYearOrderByDueDateAsc(organizationId, taxYear);
    }

    @Transactional
    public TaxFilingRecord generateFiling(TaxDtos.GenerateFilingRequest request) {
        organizationRepository.findById(request.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));

        DateRange dateRange = resolvePeriodRange(request.taxYear(), request.filingPeriod());
        List<PayrollRun> payrollRuns = payrollRunRepository.findByPayrollScheduleOrganizationIdOrderByPayDateDesc(
            request.organizationId()
        ).stream()
            .filter(run -> run.getTaxYear().equals(request.taxYear()))
            .filter(run -> run.getStatus() == PayrollRun.RunStatus.APPROVED || run.getStatus() == PayrollRun.RunStatus.POSTED)
            .filter(run -> !run.getPayDate().isBefore(dateRange.start()) && !run.getPayDate().isAfter(dateRange.end()))
            .toList();

        List<PayrollRunItem> items = payrollRuns.stream()
            .flatMap(run -> payrollRunItemRepository.findByPayrollRunIdOrderByEmployeeLastNameAscEmployeeFirstNameAsc(
                run.getId()
            ).stream())
            .toList();

        BigDecimal totalWages = items.stream()
            .map(PayrollRunItem::getGrossPay)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = switch (request.filingType()) {
            case FORM_941 -> items.stream()
                .map(item -> item.getFederalIncomeTax()
                    .add(item.getSocialSecurityEmployeeTax())
                    .add(item.getMedicareEmployeeTax())
                    .add(item.getAdditionalMedicareEmployeeTax())
                    .add(item.getEmployerSocialSecurityTax())
                    .add(item.getEmployerMedicareTax()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            case FORM_940 -> items.stream()
                .map(PayrollRunItem::getEmployerFutaTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            case FORM_W2, FORM_W3, STATE_WITHHOLDING -> items.stream()
                .map(item -> item.getFederalIncomeTax()
                    .add(item.getStateIncomeTax())
                    .add(item.getSocialSecurityEmployeeTax())
                    .add(item.getMedicareEmployeeTax())
                    .add(item.getAdditionalMedicareEmployeeTax()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        };

        TaxFilingRecord filingRecord = taxFilingRecordRepository.findByOrganizationIdAndTaxYearOrderByDueDateAsc(
            request.organizationId(),
            request.taxYear()
        ).stream()
            .filter(record -> record.getFilingType() == request.filingType())
            .filter(record -> record.getFilingPeriod().equalsIgnoreCase(request.filingPeriod()))
            .findFirst()
            .orElseGet(TaxFilingRecord::new);

        filingRecord.setOrganization(organizationRepository.getReferenceById(request.organizationId()));
        filingRecord.setFilingType(request.filingType());
        filingRecord.setTaxYear(request.taxYear());
        filingRecord.setFilingPeriod(request.filingPeriod().toUpperCase());
        filingRecord.setDueDate(request.dueDate() != null ? request.dueDate() : defaultDueDate(request.taxYear(), request.filingType(), request.filingPeriod()));
        filingRecord.setStatus(TaxFilingRecord.RecordStatus.GENERATED);
        filingRecord.setTotalWages(totalWages);
        filingRecord.setTotalTax(totalTax);
        filingRecord.setGeneratedAt(OffsetDateTime.now());
        filingRecord.setReferenceNumber(referenceNumber(request));
        return taxFilingRecordRepository.save(filingRecord);
    }

    private String referenceNumber(TaxDtos.GenerateFilingRequest request) {
        return "%s-%d-%s".formatted(
            request.filingType().name(),
            request.taxYear(),
            request.filingPeriod().toUpperCase()
        );
    }

    private LocalDate defaultDueDate(Integer taxYear, TaxFilingRecord.FilingType filingType, String filingPeriod) {
        String normalizedPeriod = filingPeriod.toUpperCase();
        return switch (filingType) {
            case FORM_941, STATE_WITHHOLDING -> switch (normalizedPeriod) {
                case "Q1" -> LocalDate.of(taxYear, Month.APRIL, 30);
                case "Q2" -> LocalDate.of(taxYear, Month.JULY, 31);
                case "Q3" -> LocalDate.of(taxYear, Month.OCTOBER, 31);
                case "Q4" -> LocalDate.of(taxYear + 1, Month.JANUARY, 31);
                default -> LocalDate.of(taxYear + 1, Month.JANUARY, 31);
            };
            case FORM_940, FORM_W2, FORM_W3 -> LocalDate.of(taxYear + 1, Month.JANUARY, 31);
        };
    }

    private DateRange resolvePeriodRange(Integer taxYear, String filingPeriod) {
        return switch (filingPeriod.toUpperCase()) {
            case "Q1" -> new DateRange(LocalDate.of(taxYear, 1, 1), LocalDate.of(taxYear, 3, 31));
            case "Q2" -> new DateRange(LocalDate.of(taxYear, 4, 1), LocalDate.of(taxYear, 6, 30));
            case "Q3" -> new DateRange(LocalDate.of(taxYear, 7, 1), LocalDate.of(taxYear, 9, 30));
            case "Q4" -> new DateRange(LocalDate.of(taxYear, 10, 1), LocalDate.of(taxYear, 12, 31));
            case "ANNUAL" -> new DateRange(LocalDate.of(taxYear, 1, 1), LocalDate.of(taxYear, 12, 31));
            default -> throw new IllegalArgumentException("Unsupported filing period: " + filingPeriod);
        };
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
