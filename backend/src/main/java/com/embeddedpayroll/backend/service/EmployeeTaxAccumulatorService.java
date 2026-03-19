package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeTaxAccumulator;
import com.embeddedpayroll.backend.repository.EmployeeTaxAccumulatorRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeTaxAccumulatorService {

    private final EmployeeTaxAccumulatorRepository employeeTaxAccumulatorRepository;

    @Transactional
    public void recordComputation(
        Employee employee,
        TaxEngineService.PayrollComputation computation
    ) {
        accumulate(
            employee,
            computation.taxYear(),
            EmployeeTaxAccumulator.TaxCode.FEDERAL_INCOME_TAX,
            "US",
            computation.taxableWages(),
            computation.federalIncomeTax(),
            BigDecimal.ZERO
        );
        accumulate(
            employee,
            computation.taxYear(),
            EmployeeTaxAccumulator.TaxCode.SOCIAL_SECURITY,
            "US",
            computation.socialSecurityTaxableWages(),
            computation.socialSecurityEmployeeTax(),
            computation.employerSocialSecurityTax()
        );
        accumulate(
            employee,
            computation.taxYear(),
            EmployeeTaxAccumulator.TaxCode.MEDICARE,
            "US",
            computation.taxableWages(),
            computation.medicareEmployeeTax(),
            computation.employerMedicareTax()
        );
        accumulate(
            employee,
            computation.taxYear(),
            EmployeeTaxAccumulator.TaxCode.ADDITIONAL_MEDICARE,
            "US",
            computation.taxableWages(),
            computation.additionalMedicareEmployeeTax(),
            BigDecimal.ZERO
        );
        accumulate(
            employee,
            computation.taxYear(),
            EmployeeTaxAccumulator.TaxCode.FUTA,
            "US",
            computation.federalUnemploymentTaxableWages(),
            BigDecimal.ZERO,
            computation.employerFutaTax()
        );
        for (TaxEngineService.StateUnemploymentAccumulator accumulator : computation.stateUnemploymentAccumulators()) {
            accumulate(
                employee,
                computation.taxYear(),
                EmployeeTaxAccumulator.TaxCode.STATE_UNEMPLOYMENT,
                accumulator.stateJurisdictionCode(),
                accumulator.taxableWages(),
                BigDecimal.ZERO,
                accumulator.employerTax()
            );
        }
        if (computation.residentStateIncomeTax().signum() > 0) {
            accumulate(
                employee,
                computation.taxYear(),
                EmployeeTaxAccumulator.TaxCode.STATE_WITHHOLDING,
                computation.residentStateJurisdictionCode(),
                computation.taxableWages(),
                computation.residentStateIncomeTax(),
                BigDecimal.ZERO
            );
        }
        for (TaxEngineService.WorkLocationAccumulator allocationAccumulator : computation.workLocationAccumulators()) {
            if (allocationAccumulator.workStateIncomeTax().signum() > 0) {
                accumulate(
                    employee,
                    computation.taxYear(),
                    EmployeeTaxAccumulator.TaxCode.STATE_WITHHOLDING,
                    allocationAccumulator.stateJurisdictionCode(),
                    allocationAccumulator.allocatedTaxableWages(),
                    allocationAccumulator.workStateIncomeTax(),
                    BigDecimal.ZERO
                );
            }
            if (allocationAccumulator.localIncomeTax().signum() > 0
                && allocationAccumulator.localJurisdictionCode() != null) {
                accumulate(
                    employee,
                    computation.taxYear(),
                    EmployeeTaxAccumulator.TaxCode.LOCAL_WITHHOLDING,
                    allocationAccumulator.localJurisdictionCode(),
                    allocationAccumulator.allocatedTaxableWages(),
                    allocationAccumulator.localIncomeTax(),
                    BigDecimal.ZERO
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal ytdTaxableWages(
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

    private void accumulate(
        Employee employee,
        Integer taxYear,
        EmployeeTaxAccumulator.TaxCode taxCode,
        String jurisdictionCode,
        BigDecimal taxableWages,
        BigDecimal employeeTax,
        BigDecimal employerTax
    ) {
        EmployeeTaxAccumulator accumulator = employeeTaxAccumulatorRepository
            .findByEmployeeIdAndTaxYearAndTaxCodeAndJurisdictionCode(
                employee.getId(),
                taxYear,
                taxCode,
                jurisdictionCode
            )
            .orElseGet(EmployeeTaxAccumulator::new);
        accumulator.setEmployee(employee);
        accumulator.setTaxYear(taxYear);
        accumulator.setTaxCode(taxCode);
        accumulator.setJurisdictionCode(jurisdictionCode);
        accumulator.setYtdTaxableWages(accumulator.getYtdTaxableWages().add(nonNull(taxableWages)));
        accumulator.setYtdEmployeeTaxAmount(accumulator.getYtdEmployeeTaxAmount().add(nonNull(employeeTax)));
        accumulator.setYtdEmployerTaxAmount(accumulator.getYtdEmployerTaxAmount().add(nonNull(employerTax)));
        employeeTaxAccumulatorRepository.save(accumulator);
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
