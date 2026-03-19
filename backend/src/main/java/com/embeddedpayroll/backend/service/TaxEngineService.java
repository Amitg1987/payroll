package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeTaxAccumulator;
import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import com.embeddedpayroll.backend.model.FederalTaxBracket;
import com.embeddedpayroll.backend.model.JurisdictionTaxProfile;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.model.StateReciprocityAgreement;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import com.embeddedpayroll.backend.repository.EmployeeTaxAccumulatorRepository;
import com.embeddedpayroll.backend.repository.JurisdictionTaxProfileRepository;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import com.embeddedpayroll.backend.repository.StateReciprocityAgreementRepository;
import com.embeddedpayroll.backend.repository.TaxYearProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.embeddedpayroll.backend.dto.PayrollDtos;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxEngineService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.50");
    private static final BigDecimal HUNDRED_PERCENT = new BigDecimal("1.0000");

    private final TaxYearProfileRepository taxYearProfileRepository;
    private final PayrollRunItemRepository payrollRunItemRepository;
    private final JurisdictionTaxProfileRepository jurisdictionTaxProfileRepository;
    private final StateReciprocityAgreementRepository stateReciprocityAgreementRepository;
    private final EmployeeTaxAccumulatorRepository employeeTaxAccumulatorRepository;

    @Transactional(readOnly = true)
    public PayrollComputation calculate(
        Employee employee,
        EmployeeW4Profile w4Profile,
        Integer taxYear,
        PayrollSchedule.PayrollFrequency frequency,
        BigDecimal bonusPay,
        BigDecimal overtimeHours,
        BigDecimal preTaxDeductions,
        List<PayrollDtos.WorkLocationAllocationRequest> workLocationAllocations
    ) {
        if (employee.getWorkerType() != Employee.WorkerType.W2_EMPLOYEE) {
            throw new IllegalArgumentException("Payroll engine currently supports W-2 employees only");
        }

        TaxYearProfile taxYearProfile = getTaxYearProfile(taxYear);
        BigDecimal regularPay = calculateRegularPay(employee, frequency);
        BigDecimal overtimePay = calculateOvertimePay(employee, overtimeHours);
        BigDecimal grossPay = money(regularPay.add(nz(bonusPay)).add(overtimePay));
        BigDecimal pretax = money(nz(preTaxDeductions));
        BigDecimal taxableWages = money(max(grossPay.subtract(pretax), BigDecimal.ZERO));
        List<ResolvedWorkLocationAllocation> resolvedAllocations = resolveAllocations(
            employee,
            grossPay,
            taxableWages,
            workLocationAllocations
        );

        BigDecimal federalIncomeTax = calculateFederalIncomeTax(taxYearProfile, w4Profile, frequency, taxableWages);
        StateTaxComputation stateTaxComputation = calculateStateTaxes(
            employee,
            taxYear,
            taxableWages,
            resolvedAllocations
        );

        BigDecimal ytdSocialSecurityWages = ytdTaxableWages(
            employee.getId(),
            taxYear,
            EmployeeTaxAccumulator.TaxCode.SOCIAL_SECURITY,
            "US"
        );
        BigDecimal socialSecurityTaxable = remainingWageBase(
            taxYearProfile.getSocialSecurityWageBase(),
            ytdSocialSecurityWages,
            taxableWages
        );
        BigDecimal socialSecurityEmployeeTax = money(
            socialSecurityTaxable.multiply(taxYearProfile.getSocialSecurityEmployeeRate())
        );
        BigDecimal socialSecurityEmployerTax = money(
            socialSecurityTaxable.multiply(taxYearProfile.getSocialSecurityEmployerRate())
        );

        BigDecimal medicareEmployeeTax = money(
            taxableWages.multiply(taxYearProfile.getMedicareEmployeeRate())
        );
        BigDecimal medicareEmployerTax = money(
            taxableWages.multiply(taxYearProfile.getMedicareEmployerRate())
        );

        BigDecimal ytdMedicareWages = ytdTaxableWages(
            employee.getId(),
            taxYear,
            EmployeeTaxAccumulator.TaxCode.MEDICARE,
            "US"
        );
        BigDecimal additionalMedicareTaxable = additionalMedicareTaxable(
            ytdMedicareWages,
            taxableWages,
            taxYearProfile.getAdditionalMedicareThreshold()
        );
        BigDecimal additionalMedicareEmployeeTax = money(
            additionalMedicareTaxable.multiply(taxYearProfile.getAdditionalMedicareRate())
        );

        BigDecimal ytdFutaWages = ytdTaxableWages(
            employee.getId(),
            taxYear,
            EmployeeTaxAccumulator.TaxCode.FUTA,
            "US"
        );
        BigDecimal futaTaxable = remainingWageBase(
            taxYearProfile.getFederalUnemploymentWageBase(),
            ytdFutaWages,
            taxableWages
        );
        BigDecimal employerFutaTax = money(
            futaTaxable.multiply(taxYearProfile.getFederalUnemploymentRate())
        );

        StateUnemploymentComputation stateUnemploymentComputation = calculateEmployerStateUnemploymentTax(
            employee,
            taxYear,
            resolvedAllocations
        );

        BigDecimal employeeTaxTotal = money(
            federalIncomeTax
                .add(socialSecurityEmployeeTax)
                .add(medicareEmployeeTax)
                .add(additionalMedicareEmployeeTax)
                .add(stateTaxComputation.totalStateIncomeTax())
                .add(stateTaxComputation.totalLocalIncomeTax())
        );
        BigDecimal netPay = money(grossPay.subtract(pretax).subtract(employeeTaxTotal));

        return new PayrollComputation(
            employee.getId(),
            employee.getFirstName() + " " + employee.getLastName(),
            taxYear,
            w4Profile.getFilingStatus(),
            regularPay,
            money(nz(bonusPay)),
            overtimePay,
            grossPay,
            pretax,
            taxableWages,
            federalIncomeTax,
            socialSecurityEmployeeTax,
            medicareEmployeeTax,
            additionalMedicareEmployeeTax,
            stateTaxComputation.totalStateIncomeTax(),
            stateTaxComputation.totalWorkStateIncomeTax(),
            stateTaxComputation.residentStateIncomeTax(),
            stateTaxComputation.residentStateCreditOffset(),
            stateTaxComputation.totalLocalIncomeTax(),
            employeeTaxTotal,
            socialSecurityEmployerTax,
            medicareEmployerTax,
            employerFutaTax,
            stateUnemploymentComputation.employerStateUnemploymentTax(),
            socialSecurityTaxable,
            futaTaxable,
            stateUnemploymentComputation.stateUnemploymentTaxableWages(),
            stateTaxComputation.primaryWorkStateJurisdictionCode(),
            stateTaxComputation.primaryLocalJurisdictionCode(),
            stateTaxComputation.residentStateJurisdictionCode(),
            stateTaxComputation.allocations().stream()
                .map(allocation -> new WorkLocationAccumulator(
                    allocation.allocation().stateJurisdictionCode(),
                    allocation.allocation().localJurisdictionCode(),
                    allocation.allocation().allocationPercentage(),
                    allocation.allocation().allocatedGrossWages(),
                    allocation.allocation().allocatedTaxableWages(),
                    allocation.workStateIncomeTax(),
                    allocation.localIncomeTax(),
                    stateUnemploymentComputation.taxableWagesForState(allocation.allocation().stateJurisdictionCode()),
                    stateUnemploymentComputation.employerTaxForState(allocation.allocation().stateJurisdictionCode()),
                    allocation.residentStateCreditApplied(),
                    allocation.reciprocityApplied()
                ))
                .toList(),
            stateUnemploymentComputation.components().stream()
                .map(component -> new StateUnemploymentAccumulator(
                    component.stateJurisdictionCode(),
                    component.taxableWages(),
                    component.employerTax()
                ))
                .toList(),
            netPay
        );
    }

    public TaxYearProfile getTaxYearProfile(Integer taxYear) {
        return taxYearProfileRepository.findByTaxYear(taxYear)
            .orElseThrow(() -> new IllegalArgumentException("No tax year profile configured for " + taxYear));
    }

    private BigDecimal calculateRegularPay(Employee employee, PayrollSchedule.PayrollFrequency frequency) {
        if (employee.getCompensationType() == Employee.CompensationType.HOURLY) {
            return money(nz(employee.getHourlyRate()).multiply(nz(employee.getStandardHoursPerPeriod())));
        }
        return money(
            nz(employee.getAnnualSalary()).divide(BigDecimal.valueOf(frequency.periodsPerYear()), 2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateOvertimePay(Employee employee, BigDecimal overtimeHours) {
        BigDecimal hours = nz(overtimeHours);
        if (hours.signum() == 0) {
            return ZERO;
        }
        BigDecimal hourlyRate = employee.getCompensationType() == Employee.CompensationType.HOURLY
            ? nz(employee.getHourlyRate())
            : nz(employee.getAnnualSalary()).divide(new BigDecimal("2080"), 6, RoundingMode.HALF_UP);
        return money(hourlyRate.multiply(hours).multiply(OVERTIME_MULTIPLIER));
    }

    private BigDecimal calculateFederalIncomeTax(
        TaxYearProfile taxYearProfile,
        EmployeeW4Profile w4Profile,
        PayrollSchedule.PayrollFrequency frequency,
        BigDecimal taxableWages
    ) {
        if (w4Profile.isExemptFromWithholding()) {
            return ZERO;
        }
        int periods = frequency.periodsPerYear();
        BigDecimal standardDeduction = standardDeduction(taxYearProfile, w4Profile.getFilingStatus());
        BigDecimal annualizedTaxable = taxableWages.multiply(BigDecimal.valueOf(periods))
            .add(nz(w4Profile.getOtherIncome()))
            .subtract(standardDeduction)
            .subtract(nz(w4Profile.getDeductions()));
        annualizedTaxable = max(annualizedTaxable, BigDecimal.ZERO);
        BigDecimal taxableAnnualIncome = annualizedTaxable;

        BigDecimal annualFederalTax = federalTaxBrackets(taxYearProfile, w4Profile.getFilingStatus()).stream()
            .map(bracket -> taxForBracket(taxableAnnualIncome, bracket))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .subtract(nz(w4Profile.getDependentsCredit()));

        if (w4Profile.isMultipleJobs()) {
            annualFederalTax = annualFederalTax.multiply(new BigDecimal("1.10"));
        }
        annualFederalTax = max(annualFederalTax, BigDecimal.ZERO);
        BigDecimal perPeriodTax = annualFederalTax.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
        return money(perPeriodTax.add(nz(w4Profile.getExtraWithholding())));
    }

    private List<FederalTaxBracket> federalTaxBrackets(
        TaxYearProfile taxYearProfile,
        EmployeeW4Profile.FilingStatus filingStatus
    ) {
        return taxYearProfile.getFederalTaxBrackets().stream()
            .filter(bracket -> bracket.getFilingStatus() == filingStatus)
            .sorted(Comparator.comparing(FederalTaxBracket::getBracketOrder))
            .toList();
    }

    private BigDecimal taxForBracket(BigDecimal annualizedTaxable, FederalTaxBracket bracket) {
        BigDecimal upperBound = bracket.getUpperBound() == null ? annualizedTaxable : bracket.getUpperBound();
        BigDecimal taxableAtBracket = max(BigDecimal.ZERO, annualizedTaxable.min(upperBound).subtract(bracket.getLowerBound()));
        return money(taxableAtBracket.multiply(bracket.getRate()));
    }

    private BigDecimal additionalMedicareTaxable(
        BigDecimal yearToDateGross,
        BigDecimal taxableWages,
        BigDecimal threshold
    ) {
        BigDecimal priorExcess = max(BigDecimal.ZERO, yearToDateGross.subtract(threshold));
        BigDecimal currentExcess = max(BigDecimal.ZERO, yearToDateGross.add(taxableWages).subtract(threshold));
        return money(max(BigDecimal.ZERO, currentExcess.subtract(priorExcess)));
    }

    private BigDecimal standardDeduction(
        TaxYearProfile taxYearProfile,
        EmployeeW4Profile.FilingStatus filingStatus
    ) {
        return switch (filingStatus) {
            case SINGLE -> taxYearProfile.getStandardDeductionSingle();
            case MARRIED_FILING_JOINTLY -> taxYearProfile.getStandardDeductionMarriedJointly();
            case HEAD_OF_HOUSEHOLD -> taxYearProfile.getStandardDeductionHeadOfHousehold();
        };
    }

    private StateTaxComputation calculateStateTaxes(
        Employee employee,
        Integer taxYear,
        BigDecimal taxableWages,
        List<ResolvedWorkLocationAllocation> allocations
    ) {
        JurisdictionTaxProfile residentStateProfile = jurisdictionTaxProfileRepository
            .findByTaxJurisdiction_CodeAndTaxYearAndTaxType(
                employee.getResidenceState(),
                taxYear,
                JurisdictionTaxProfile.TaxType.STATE_WITHHOLDING
            )
            .orElse(null);

        List<WorkLocationTaxBreakdown> breakdowns = allocations.stream()
            .map(allocation -> {
                boolean reciprocityApplied = hasReciprocity(employee.getResidenceState(), allocation.stateJurisdictionCode());
                BigDecimal workStateIncomeTax = calculateWorkStateIncomeTax(
                    employee,
                    taxYear,
                    allocation,
                    reciprocityApplied
                );
                BigDecimal localIncomeTax = calculateLocalIncomeTaxForAllocation(
                    employee,
                    taxYear,
                    allocation
                );
                return new WorkLocationTaxBreakdown(
                    allocation,
                    workStateIncomeTax,
                    localIncomeTax,
                    reciprocityApplied,
                    ZERO
                );
            })
            .toList();

        BigDecimal totalWorkStateIncomeTax = breakdowns.stream()
            .map(WorkLocationTaxBreakdown::workStateIncomeTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal residentStateGrossLiability = residentStateProfile == null
            ? ZERO
            : money(taxableWages.multiply(nz(residentStateProfile.getResidentRate())));
        BigDecimal residentStateCreditOffset = money(residentStateGrossLiability.min(totalWorkStateIncomeTax));
        BigDecimal residentStateIncomeTax = money(residentStateGrossLiability.subtract(residentStateCreditOffset));

        List<WorkLocationTaxBreakdown> creditAppliedBreakdowns = applyResidentCredits(
            breakdowns,
            residentStateCreditOffset
        );

        BigDecimal totalLocalIncomeTax = creditAppliedBreakdowns.stream()
            .map(WorkLocationTaxBreakdown::localIncomeTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalStateIncomeTax = money(totalWorkStateIncomeTax.add(residentStateIncomeTax));

        return new StateTaxComputation(
            totalStateIncomeTax,
            totalWorkStateIncomeTax,
            residentStateIncomeTax,
            residentStateCreditOffset,
            totalLocalIncomeTax,
            residentStateProfile == null ? null : employee.getResidenceState(),
            primaryWorkStateCode(allocations, employee),
            primaryLocalJurisdictionCode(allocations),
            creditAppliedBreakdowns
        );
    }

    private StateUnemploymentComputation calculateEmployerStateUnemploymentTax(
        Employee employee,
        Integer taxYear,
        List<ResolvedWorkLocationAllocation> allocations
    ) {
        java.util.ArrayList<StateUnemploymentComponent> components = new java.util.ArrayList<>();
        BigDecimal totalEmployerStateUnemploymentTax = BigDecimal.ZERO;
        BigDecimal totalStateUnemploymentTaxableWages = BigDecimal.ZERO;

        for (ResolvedWorkLocationAllocation allocation : allocations) {
            JurisdictionTaxProfile profile = jurisdictionTaxProfileRepository
                .findByTaxJurisdiction_CodeAndTaxYearAndTaxType(
                    allocation.stateJurisdictionCode(),
                    taxYear,
                    JurisdictionTaxProfile.TaxType.STATE_UNEMPLOYMENT
                )
                .orElse(null);
            if (profile == null || nz(profile.getEmployerRate()).signum() == 0) {
                continue;
            }

            BigDecimal ytdStateUnemploymentWages = ytdTaxableWages(
                employee.getId(),
                taxYear,
                EmployeeTaxAccumulator.TaxCode.STATE_UNEMPLOYMENT,
                allocation.stateJurisdictionCode()
            );
            BigDecimal unemploymentTaxable = profile.getWageBase() == null
                ? allocation.allocatedTaxableWages()
                : remainingWageBase(
                    profile.getWageBase(),
                    ytdStateUnemploymentWages,
                    allocation.allocatedTaxableWages()
                );
            BigDecimal employerTax = money(unemploymentTaxable.multiply(nz(profile.getEmployerRate())));
            components.add(new StateUnemploymentComponent(
                allocation.stateJurisdictionCode(),
                unemploymentTaxable,
                employerTax
            ));
            totalEmployerStateUnemploymentTax = totalEmployerStateUnemploymentTax.add(employerTax);
            totalStateUnemploymentTaxableWages = totalStateUnemploymentTaxableWages.add(unemploymentTaxable);
        }

        return new StateUnemploymentComputation(
            money(totalEmployerStateUnemploymentTax),
            money(totalStateUnemploymentTaxableWages),
            List.copyOf(components)
        );
    }

    private List<ResolvedWorkLocationAllocation> resolveAllocations(
        Employee employee,
        BigDecimal grossPay,
        BigDecimal taxableWages,
        List<PayrollDtos.WorkLocationAllocationRequest> requestedAllocations
    ) {
        List<PayrollDtos.WorkLocationAllocationRequest> effectiveAllocations =
            requestedAllocations == null || requestedAllocations.isEmpty()
                ? List.of(new PayrollDtos.WorkLocationAllocationRequest(
                    employee.getWorkState(),
                    employee.getWorkLocalJurisdictionCode(),
                    HUNDRED_PERCENT
                ))
                : requestedAllocations;

        validateAllocationPercentages(effectiveAllocations);

        BigDecimal remainingGross = grossPay;
        BigDecimal remainingTaxable = taxableWages;
        java.util.ArrayList<ResolvedWorkLocationAllocation> resolved = new java.util.ArrayList<>();

        for (int index = 0; index < effectiveAllocations.size(); index++) {
            PayrollDtos.WorkLocationAllocationRequest allocation = effectiveAllocations.get(index);
            BigDecimal allocationPercentage = allocation.allocationPercentage().setScale(4, RoundingMode.HALF_UP);
            BigDecimal allocatedGrossWages = index == effectiveAllocations.size() - 1
                ? remainingGross
                : money(grossPay.multiply(allocationPercentage));
            BigDecimal allocatedTaxableWages = index == effectiveAllocations.size() - 1
                ? remainingTaxable
                : money(taxableWages.multiply(allocationPercentage));
            resolved.add(new ResolvedWorkLocationAllocation(
                allocation.stateJurisdictionCode(),
                allocation.localJurisdictionCode(),
                allocationPercentage,
                allocatedGrossWages,
                allocatedTaxableWages
            ));
            remainingGross = money(remainingGross.subtract(allocatedGrossWages));
            remainingTaxable = money(remainingTaxable.subtract(allocatedTaxableWages));
        }

        return List.copyOf(resolved);
    }

    private void validateAllocationPercentages(List<PayrollDtos.WorkLocationAllocationRequest> allocations) {
        BigDecimal total = allocations.stream()
            .map(PayrollDtos.WorkLocationAllocationRequest::allocationPercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);
        if (total.compareTo(HUNDRED_PERCENT) != 0) {
            throw new IllegalArgumentException("Work location allocation percentages must total 1.0000");
        }
    }

    private BigDecimal calculateWorkStateIncomeTax(
        Employee employee,
        Integer taxYear,
        ResolvedWorkLocationAllocation allocation,
        boolean reciprocityApplied
    ) {
        if (allocation.stateJurisdictionCode().equalsIgnoreCase(employee.getResidenceState()) || reciprocityApplied) {
            return ZERO;
        }
        JurisdictionTaxProfile workStateProfile = jurisdictionTaxProfileRepository
            .findByTaxJurisdiction_CodeAndTaxYearAndTaxType(
                allocation.stateJurisdictionCode(),
                taxYear,
                JurisdictionTaxProfile.TaxType.STATE_WITHHOLDING
            )
            .orElse(null);
        if (workStateProfile == null) {
            return money(allocation.allocatedTaxableWages().multiply(nz(employee.getStateWithholdingRate())));
        }
        return money(allocation.allocatedTaxableWages().multiply(nz(workStateProfile.getNonResidentRate())));
    }

    private BigDecimal calculateLocalIncomeTaxForAllocation(
        Employee employee,
        Integer taxYear,
        ResolvedWorkLocationAllocation allocation
    ) {
        if (allocation.localJurisdictionCode() == null || allocation.localJurisdictionCode().isBlank()) {
            return ZERO;
        }
        JurisdictionTaxProfile profile = jurisdictionTaxProfileRepository
            .findByTaxJurisdiction_CodeAndTaxYearAndTaxType(
                allocation.localJurisdictionCode(),
                taxYear,
                JurisdictionTaxProfile.TaxType.LOCAL_WITHHOLDING
            )
            .orElse(null);
        if (profile == null) {
            return ZERO;
        }
        boolean resident = allocation.localJurisdictionCode().equalsIgnoreCase(employee.getResidenceLocalJurisdictionCode());
        BigDecimal rate = resident ? nz(profile.getResidentRate()) : nz(profile.getNonResidentRate());
        return money(allocation.allocatedTaxableWages().multiply(rate));
    }

    private List<WorkLocationTaxBreakdown> applyResidentCredits(
        List<WorkLocationTaxBreakdown> breakdowns,
        BigDecimal residentStateCreditOffset
    ) {
        BigDecimal totalExternalTax = breakdowns.stream()
            .map(WorkLocationTaxBreakdown::workStateIncomeTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (residentStateCreditOffset.signum() == 0 || totalExternalTax.signum() == 0) {
            return breakdowns;
        }

        BigDecimal remainingCredit = residentStateCreditOffset;
        int lastTaxableIndex = -1;
        for (int index = 0; index < breakdowns.size(); index++) {
            if (breakdowns.get(index).workStateIncomeTax().signum() > 0) {
                lastTaxableIndex = index;
            }
        }
        java.util.ArrayList<WorkLocationTaxBreakdown> adjusted = new java.util.ArrayList<>();
        for (int index = 0; index < breakdowns.size(); index++) {
            WorkLocationTaxBreakdown breakdown = breakdowns.get(index);
            BigDecimal credit = breakdown.workStateIncomeTax().signum() == 0
                ? ZERO
                : index == lastTaxableIndex
                    ? remainingCredit
                    : money(
                        residentStateCreditOffset.multiply(
                            breakdown.workStateIncomeTax().divide(totalExternalTax, 8, RoundingMode.HALF_UP)
                        )
                    );
            adjusted.add(new WorkLocationTaxBreakdown(
                breakdown.allocation(),
                breakdown.workStateIncomeTax(),
                breakdown.localIncomeTax(),
                breakdown.reciprocityApplied(),
                credit
            ));
            remainingCredit = money(remainingCredit.subtract(credit));
        }
        return List.copyOf(adjusted);
    }

    private boolean hasReciprocity(String residentStateCode, String workStateCode) {
        if (residentStateCode.equalsIgnoreCase(workStateCode)) {
            return false;
        }
        return stateReciprocityAgreementRepository.findByResidentStateCodeAndWorkStateCodeAndActiveTrue(
            residentStateCode,
            workStateCode
        ).isPresent();
    }

    private String primaryWorkStateCode(List<ResolvedWorkLocationAllocation> allocations, Employee employee) {
        return allocations.stream()
            .max(java.util.Comparator.comparing(ResolvedWorkLocationAllocation::allocatedTaxableWages))
            .map(ResolvedWorkLocationAllocation::stateJurisdictionCode)
            .orElse(employee.getWorkState());
    }

    private String primaryLocalJurisdictionCode(List<ResolvedWorkLocationAllocation> allocations) {
        return allocations.stream()
            .map(ResolvedWorkLocationAllocation::localJurisdictionCode)
            .filter(code -> code != null && !code.isBlank())
            .findFirst()
            .orElse(null);
    }

    private BigDecimal ytdTaxableWages(
        Long employeeId,
        Integer taxYear,
        EmployeeTaxAccumulator.TaxCode taxCode,
        String jurisdictionCode
    ) {
        return employeeTaxAccumulatorRepository.findByEmployeeIdAndTaxYearAndTaxCodeAndJurisdictionCode(
            employeeId,
            taxYear,
            taxCode,
            jurisdictionCode
        )
            .map(EmployeeTaxAccumulator::getYtdTaxableWages)
            .orElse(BigDecimal.ZERO);
    }

    private BigDecimal remainingWageBase(
        BigDecimal wageBase,
        BigDecimal ytdTaxableWages,
        BigDecimal currentTaxableWages
    ) {
        return max(BigDecimal.ZERO, wageBase.subtract(ytdTaxableWages)).min(currentTaxableWages);
    }

    private BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        return left.max(right);
    }

    public record PayrollComputation(
        Long employeeId,
        String employeeName,
        Integer taxYear,
        EmployeeW4Profile.FilingStatus filingStatus,
        BigDecimal regularPay,
        BigDecimal bonusPay,
        BigDecimal overtimePay,
        BigDecimal grossPay,
        BigDecimal preTaxDeductions,
        BigDecimal taxableWages,
        BigDecimal federalIncomeTax,
        BigDecimal socialSecurityEmployeeTax,
        BigDecimal medicareEmployeeTax,
        BigDecimal additionalMedicareEmployeeTax,
        BigDecimal stateIncomeTax,
        BigDecimal workStateIncomeTax,
        BigDecimal residentStateIncomeTax,
        BigDecimal residentStateCreditOffset,
        BigDecimal localIncomeTax,
        BigDecimal employeeTaxTotal,
        BigDecimal employerSocialSecurityTax,
        BigDecimal employerMedicareTax,
        BigDecimal employerFutaTax,
        BigDecimal employerStateUnemploymentTax,
        BigDecimal socialSecurityTaxableWages,
        BigDecimal federalUnemploymentTaxableWages,
        BigDecimal stateUnemploymentTaxableWages,
        String stateJurisdictionCode,
        String localJurisdictionCode,
        String residentStateJurisdictionCode,
        List<WorkLocationAccumulator> workLocationAccumulators,
        List<StateUnemploymentAccumulator> stateUnemploymentAccumulators,
        BigDecimal netPay
    ) {
    }

    public record WorkLocationAccumulator(
        String stateJurisdictionCode,
        String localJurisdictionCode,
        BigDecimal allocationPercentage,
        BigDecimal allocatedGrossWages,
        BigDecimal allocatedTaxableWages,
        BigDecimal workStateIncomeTax,
        BigDecimal localIncomeTax,
        BigDecimal stateUnemploymentTaxableWages,
        BigDecimal employerStateUnemploymentTax,
        BigDecimal residentStateCreditApplied,
        boolean reciprocityApplied
    ) {
    }

    public record StateUnemploymentAccumulator(
        String stateJurisdictionCode,
        BigDecimal taxableWages,
        BigDecimal employerTax
    ) {
    }

    private record ResolvedWorkLocationAllocation(
        String stateJurisdictionCode,
        String localJurisdictionCode,
        BigDecimal allocationPercentage,
        BigDecimal allocatedGrossWages,
        BigDecimal allocatedTaxableWages
    ) {
    }

    private record WorkLocationTaxBreakdown(
        ResolvedWorkLocationAllocation allocation,
        BigDecimal workStateIncomeTax,
        BigDecimal localIncomeTax,
        boolean reciprocityApplied,
        BigDecimal residentStateCreditApplied
    ) {
    }

    private record StateTaxComputation(
        BigDecimal totalStateIncomeTax,
        BigDecimal totalWorkStateIncomeTax,
        BigDecimal residentStateIncomeTax,
        BigDecimal residentStateCreditOffset,
        BigDecimal totalLocalIncomeTax,
        String residentStateJurisdictionCode,
        String primaryWorkStateJurisdictionCode,
        String primaryLocalJurisdictionCode,
        List<WorkLocationTaxBreakdown> allocations
    ) {
    }

    private record StateUnemploymentComputation(
        BigDecimal employerStateUnemploymentTax,
        BigDecimal stateUnemploymentTaxableWages,
        List<StateUnemploymentComponent> components
    ) {
        private BigDecimal taxableWagesForState(String stateJurisdictionCode) {
            return components.stream()
                .filter(component -> component.stateJurisdictionCode().equalsIgnoreCase(stateJurisdictionCode))
                .map(StateUnemploymentComponent::taxableWages)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        }

        private BigDecimal employerTaxForState(String stateJurisdictionCode) {
            return components.stream()
                .filter(component -> component.stateJurisdictionCode().equalsIgnoreCase(stateJurisdictionCode))
                .map(StateUnemploymentComponent::employerTax)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        }
    }

    private record StateUnemploymentComponent(
        String stateJurisdictionCode,
        BigDecimal taxableWages,
        BigDecimal employerTax
    ) {
    }
}
