package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import com.embeddedpayroll.backend.model.FederalTaxBracket;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import com.embeddedpayroll.backend.repository.TaxYearProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxEngineService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.50");

    private final TaxYearProfileRepository taxYearProfileRepository;
    private final PayrollRunItemRepository payrollRunItemRepository;

    public PayrollComputation calculate(
        Employee employee,
        EmployeeW4Profile w4Profile,
        Integer taxYear,
        PayrollSchedule.PayrollFrequency frequency,
        BigDecimal bonusPay,
        BigDecimal overtimeHours,
        BigDecimal preTaxDeductions
    ) {
        TaxYearProfile taxYearProfile = getTaxYearProfile(taxYear);
        BigDecimal regularPay = calculateRegularPay(employee, frequency);
        BigDecimal overtimePay = calculateOvertimePay(employee, overtimeHours);
        BigDecimal grossPay = money(regularPay.add(nz(bonusPay)).add(overtimePay));
        BigDecimal pretax = money(nz(preTaxDeductions));
        BigDecimal taxableWages = money(max(grossPay.subtract(pretax), BigDecimal.ZERO));
        BigDecimal yearToDateGross = money(
            payrollRunItemRepository.sumGrossPayForEmployeeAndTaxYear(employee.getId(), taxYear)
        );

        BigDecimal federalIncomeTax = calculateFederalIncomeTax(taxYearProfile, w4Profile, frequency, taxableWages);
        BigDecimal stateIncomeTax = money(taxableWages.multiply(nz(employee.getStateWithholdingRate())));

        BigDecimal socialSecurityTaxable = max(
            BigDecimal.ZERO,
            taxYearProfile.getSocialSecurityWageBase().subtract(yearToDateGross)
        ).min(taxableWages);
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

        BigDecimal additionalMedicareTaxable = additionalMedicareTaxable(
            yearToDateGross,
            taxableWages,
            taxYearProfile.getAdditionalMedicareThreshold()
        );
        BigDecimal additionalMedicareEmployeeTax = money(
            additionalMedicareTaxable.multiply(taxYearProfile.getAdditionalMedicareRate())
        );

        BigDecimal futaTaxable = max(
            BigDecimal.ZERO,
            taxYearProfile.getFederalUnemploymentWageBase().subtract(yearToDateGross)
        ).min(taxableWages);
        BigDecimal employerFutaTax = money(
            futaTaxable.multiply(taxYearProfile.getFederalUnemploymentRate())
        );

        BigDecimal employeeTaxTotal = money(
            federalIncomeTax
                .add(socialSecurityEmployeeTax)
                .add(medicareEmployeeTax)
                .add(additionalMedicareEmployeeTax)
                .add(stateIncomeTax)
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
            stateIncomeTax,
            employeeTaxTotal,
            socialSecurityEmployerTax,
            medicareEmployerTax,
            employerFutaTax,
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

        BigDecimal annualFederalTax = federalTaxBrackets(taxYearProfile, w4Profile.getFilingStatus()).stream()
            .map(bracket -> taxForBracket(annualizedTaxable, bracket))
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
        BigDecimal employeeTaxTotal,
        BigDecimal employerSocialSecurityTax,
        BigDecimal employerMedicareTax,
        BigDecimal employerFutaTax,
        BigDecimal netPay
    ) {
    }
}
