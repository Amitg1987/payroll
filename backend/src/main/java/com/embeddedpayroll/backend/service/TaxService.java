package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.JurisdictionTaxProfile;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.PayrollRun;
import com.embeddedpayroll.backend.model.PayrollRunItem;
import com.embeddedpayroll.backend.model.PayrollRunItemAllocation;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.model.TaxJurisdiction;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import com.embeddedpayroll.backend.repository.JurisdictionTaxProfileRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.PayrollRunItemAllocationRepository;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import com.embeddedpayroll.backend.repository.PayrollRunRepository;
import com.embeddedpayroll.backend.repository.TaxFilingRecordRepository;
import com.embeddedpayroll.backend.repository.TaxJurisdictionRepository;
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
    private final PayrollRunItemAllocationRepository payrollRunItemAllocationRepository;
    private final TaxFilingRecordRepository taxFilingRecordRepository;
    private final TaxJurisdictionRepository taxJurisdictionRepository;
    private final JurisdictionTaxProfileRepository jurisdictionTaxProfileRepository;
    private final WebhookService webhookService;

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
        return generateFiling(request, "system", null, null);
    }

    @Transactional(readOnly = true)
    public List<TaxJurisdiction> listJurisdictions() {
        return taxJurisdictionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<JurisdictionTaxProfile> listJurisdictionProfiles(
        Integer taxYear,
        JurisdictionTaxProfile.TaxType taxType
    ) {
        return jurisdictionTaxProfileRepository.findByTaxYearAndTaxTypeOrderByTaxJurisdiction_NameAsc(taxYear, taxType);
    }

    @Transactional
    public TaxFilingRecord generateFiling(
        TaxDtos.GenerateFilingRequest request,
        String requestedBy,
        String workflowId,
        String workflowRunId
    ) {
        Organization organization = organizationRepository.findById(request.organizationId())
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
        List<PayrollRunItemAllocation> allocations = items.stream()
            .flatMap(item -> payrollRunItemAllocationRepository
                .findByPayrollRunItemIdOrderByStateJurisdictionCodeAscLocalJurisdictionCodeAsc(item.getId())
                .stream())
            .toList();

        BigDecimal totalWages = switch (request.filingType()) {
            case STATE_WITHHOLDING -> totalStateFilingWages(items, allocations, resolveFilingJurisdictionCode(request, organization));
            case LOCAL_WITHHOLDING -> totalLocalFilingWages(allocations, resolveFilingJurisdictionCode(request, organization));
            default -> items.stream().map(PayrollRunItem::getGrossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        };
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
                    .add(item.getLocalIncomeTax())
                    .add(item.getSocialSecurityEmployeeTax())
                    .add(item.getMedicareEmployeeTax())
                    .add(item.getAdditionalMedicareEmployeeTax()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            case LOCAL_WITHHOLDING -> totalLocalFilingTax(allocations, resolveFilingJurisdictionCode(request, organization));
        };
        if (request.filingType() == TaxFilingRecord.FilingType.STATE_WITHHOLDING) {
            totalTax = totalStateFilingTax(items, allocations, resolveFilingJurisdictionCode(request, organization));
        }

        TaxFilingRecord filingRecord = taxFilingRecordRepository.findByOrganizationIdAndTaxYearOrderByDueDateAsc(
            request.organizationId(),
            request.taxYear()
        ).stream()
            .filter(record -> record.getFilingType() == request.filingType())
            .filter(record -> record.getFilingPeriod().equalsIgnoreCase(request.filingPeriod()))
            .findFirst()
            .orElseGet(TaxFilingRecord::new);

        filingRecord.setOrganization(organization);
        filingRecord.setFilingType(request.filingType());
        filingRecord.setTaxYear(request.taxYear());
        filingRecord.setFilingPeriod(request.filingPeriod().toUpperCase());
        filingRecord.setFilingJurisdictionCode(resolveFilingJurisdictionCode(request, organization));
        filingRecord.setDueDate(request.dueDate() != null ? request.dueDate() : defaultDueDate(request.taxYear(), request.filingType(), request.filingPeriod()));
        filingRecord.setStatus(TaxFilingRecord.RecordStatus.GENERATED);
        filingRecord.setTotalWages(totalWages);
        filingRecord.setTotalTax(totalTax);
        filingRecord.setGeneratedAt(OffsetDateTime.now());
        filingRecord.setReferenceNumber(referenceNumber(request));
        filingRecord.setTemporalWorkflowId(workflowId);
        filingRecord.setTemporalRunId(workflowRunId);
        TaxFilingRecord savedRecord = taxFilingRecordRepository.save(filingRecord);
        webhookService.enqueueEvent(
            request.organizationId(),
            "tax.filing.generated",
            savedRecord.getReferenceNumber(),
            TaxDtos.fromEntity(savedRecord)
        );
        return savedRecord;
    }

    private String referenceNumber(TaxDtos.GenerateFilingRequest request) {
        return "%s-%d-%s".formatted(
            request.filingType().name(),
            request.taxYear(),
            request.filingPeriod().toUpperCase()
        );
    }

    private String resolveFilingJurisdictionCode(TaxDtos.GenerateFilingRequest request, Organization organization) {
        if (request.filingJurisdictionCode() != null && !request.filingJurisdictionCode().isBlank()) {
            return request.filingJurisdictionCode();
        }
        return switch (request.filingType()) {
            case STATE_WITHHOLDING -> organization.getPrimaryJurisdictionCode();
            case LOCAL_WITHHOLDING -> organization.getHeadquartersLocalJurisdictionCode() == null
                ? organization.getPrimaryJurisdictionCode()
                : organization.getHeadquartersLocalJurisdictionCode();
            default -> "US";
        };
    }

    private LocalDate defaultDueDate(Integer taxYear, TaxFilingRecord.FilingType filingType, String filingPeriod) {
        String normalizedPeriod = filingPeriod.toUpperCase();
        return switch (filingType) {
            case FORM_941, STATE_WITHHOLDING, LOCAL_WITHHOLDING -> switch (normalizedPeriod) {
                case "Q1" -> LocalDate.of(taxYear, Month.APRIL, 30);
                case "Q2" -> LocalDate.of(taxYear, Month.JULY, 31);
                case "Q3" -> LocalDate.of(taxYear, Month.OCTOBER, 31);
                case "Q4" -> LocalDate.of(taxYear + 1, Month.JANUARY, 31);
                default -> LocalDate.of(taxYear + 1, Month.JANUARY, 31);
            };
            case FORM_940, FORM_W2, FORM_W3 -> LocalDate.of(taxYear + 1, Month.JANUARY, 31);
        };
    }

    private BigDecimal totalStateFilingWages(
        List<PayrollRunItem> items,
        List<PayrollRunItemAllocation> allocations,
        String jurisdictionCode
    ) {
        BigDecimal residentWages = items.stream()
            .filter(item -> jurisdictionCode.equalsIgnoreCase(item.getResidentStateJurisdictionCode()))
            .map(PayrollRunItem::getTaxableWages)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal workStateWages = allocations.stream()
            .filter(allocation -> jurisdictionCode.equalsIgnoreCase(allocation.getStateJurisdictionCode()))
            .filter(allocation -> allocation.getWorkStateIncomeTax().signum() > 0)
            .map(PayrollRunItemAllocation::getAllocatedTaxableWages)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return residentWages.add(workStateWages);
    }

    private BigDecimal totalStateFilingTax(
        List<PayrollRunItem> items,
        List<PayrollRunItemAllocation> allocations,
        String jurisdictionCode
    ) {
        BigDecimal residentTax = items.stream()
            .filter(item -> jurisdictionCode.equalsIgnoreCase(item.getResidentStateJurisdictionCode()))
            .map(PayrollRunItem::getResidentStateIncomeTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal workStateTax = allocations.stream()
            .filter(allocation -> jurisdictionCode.equalsIgnoreCase(allocation.getStateJurisdictionCode()))
            .map(PayrollRunItemAllocation::getWorkStateIncomeTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return residentTax.add(workStateTax);
    }

    private BigDecimal totalLocalFilingWages(
        List<PayrollRunItemAllocation> allocations,
        String jurisdictionCode
    ) {
        return allocations.stream()
            .filter(allocation -> jurisdictionCode.equalsIgnoreCase(allocation.getLocalJurisdictionCode()))
            .map(PayrollRunItemAllocation::getAllocatedTaxableWages)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalLocalFilingTax(
        List<PayrollRunItemAllocation> allocations,
        String jurisdictionCode
    ) {
        return allocations.stream()
            .filter(allocation -> jurisdictionCode.equalsIgnoreCase(allocation.getLocalJurisdictionCode()))
            .map(PayrollRunItemAllocation::getLocalIncomeTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
