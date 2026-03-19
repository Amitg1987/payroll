package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.EmployeeDtos;
import com.embeddedpayroll.backend.dto.PayrollDtos;
import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.PayrollRun;
import com.embeddedpayroll.backend.model.PayrollRunItem;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.EmployeeW4ProfileRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.PayrollRunItemRepository;
import com.embeddedpayroll.backend.repository.PayrollRunRepository;
import com.embeddedpayroll.backend.repository.PayrollScheduleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeW4ProfileRepository employeeW4ProfileRepository;
    private final PayrollScheduleRepository payrollScheduleRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunItemRepository payrollRunItemRepository;
    private final TaxEngineService taxEngineService;

    @Transactional(readOnly = true)
    public List<Employee> listEmployees(Long organizationId) {
        return employeeRepository.findByOrganizationIdOrderByLastNameAscFirstNameAsc(organizationId);
    }

    @Transactional
    public Employee createEmployee(EmployeeDtos.CreateEmployeeRequest request) {
        Organization organization = organizationRepository.findById(request.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));

        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setEmployeeNumber(request.employeeNumber());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setSsnLastFour(request.ssnLastFour());
        employee.setHireDate(request.hireDate());
        employee.setEmploymentStatus(request.employmentStatus());
        employee.setCompensationType(request.compensationType());
        employee.setDepartment(request.department());
        employee.setWorkState(request.workState());
        employee.setAnnualSalary(request.annualSalary());
        employee.setHourlyRate(request.hourlyRate());
        employee.setStandardHoursPerPeriod(request.standardHoursPerPeriod());
        employee.setStateWithholdingRate(request.stateWithholdingRate());

        validateCompensation(employee);
        Employee savedEmployee = employeeRepository.save(employee);
        upsertW4(savedEmployee.getId(), request.w4Profile());
        return savedEmployee;
    }

    @Transactional
    public EmployeeW4Profile upsertW4(Long employeeId, EmployeeDtos.W4Request request) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        EmployeeW4Profile profile = employeeW4ProfileRepository.findByEmployeeIdAndTaxYear(employeeId, request.taxYear())
            .orElseGet(EmployeeW4Profile::new);
        profile.setEmployee(employee);
        profile.setTaxYear(request.taxYear());
        profile.setFilingStatus(request.filingStatus());
        profile.setMultipleJobs(request.multipleJobs());
        profile.setDependentsCredit(request.dependentsCredit());
        profile.setOtherIncome(request.otherIncome());
        profile.setDeductions(request.deductions());
        profile.setExtraWithholding(request.extraWithholding());
        profile.setExemptFromWithholding(request.exemptFromWithholding());
        return employeeW4ProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public EmployeeW4Profile findCurrentW4(Long employeeId, Integer taxYear) {
        return employeeW4ProfileRepository.findTopByEmployeeIdAndTaxYearLessThanEqualOrderByTaxYearDesc(employeeId, taxYear)
            .orElseThrow(() -> new IllegalArgumentException(
                "No W-4 profile found for employee " + employeeId + " and tax year " + taxYear
            ));
    }

    @Transactional(readOnly = true)
    public List<PayrollSchedule> listSchedules(Long organizationId) {
        return payrollScheduleRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId);
    }

    @Transactional
    public PayrollSchedule createSchedule(PayrollDtos.CreateScheduleRequest request) {
        Organization organization = organizationRepository.findById(request.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));
        PayrollSchedule schedule = new PayrollSchedule();
        schedule.setOrganization(organization);
        schedule.setName(request.name());
        schedule.setFrequency(request.frequency());
        schedule.setNextPayDate(request.nextPayDate());
        schedule.setApprovalRequired(request.approvalRequired());
        schedule.setActive(true);
        return payrollScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<PayrollRun> listRuns(Long organizationId) {
        return payrollRunRepository.findByPayrollScheduleOrganizationIdOrderByPayDateDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public PayrollRun getRun(Long runId) {
        return payrollRunRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + runId));
    }

    @Transactional(readOnly = true)
    public List<PayrollRunItem> listRunItems(Long payrollRunId) {
        return payrollRunItemRepository.findByPayrollRunIdOrderByEmployeeLastNameAscEmployeeFirstNameAsc(payrollRunId);
    }

    @Transactional
    public PayrollRun processSchedule(
        Long scheduleId,
        PayrollDtos.ProcessScheduleRequest request,
        String createdBy
    ) {
        PayrollSchedule schedule = payrollScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Payroll schedule not found: " + scheduleId));
        LocalDate payDate = request.payDate() != null ? request.payDate() : schedule.getNextPayDate();
        int taxYear = request.taxYear() != null ? request.taxYear() : payDate.getYear();
        LocalDate periodEnd = payDate.minusDays(1);
        LocalDate periodStart = periodEnd.minusDays(schedule.getFrequency().nominalDays() - 1L);

        List<Employee> employees = employeeRepository.findByOrganizationIdOrderByLastNameAscFirstNameAsc(
            schedule.getOrganization().getId()
        ).stream().filter(employee -> employee.getEmploymentStatus() == Employee.EmploymentStatus.ACTIVE).toList();
        if (employees.isEmpty()) {
            throw new IllegalArgumentException("No active employees assigned to organization " + schedule.getOrganization().getId());
        }

        Map<Long, PayrollDtos.EmployeeAdjustmentRequest> adjustmentMap = request.adjustments() == null
            ? Map.of()
            : request.adjustments().stream().collect(Collectors.toMap(
                PayrollDtos.EmployeeAdjustmentRequest::employeeId,
                Function.identity()
            ));

        PayrollRun run = new PayrollRun();
        run.setPayrollSchedule(schedule);
        run.setPeriodStart(periodStart);
        run.setPeriodEnd(periodEnd);
        run.setPayDate(payDate);
        run.setTaxYear(taxYear);
        run.setCreatedBy(createdBy);
        run.setStatus(schedule.isApprovalRequired() ? PayrollRun.RunStatus.PENDING_APPROVAL : PayrollRun.RunStatus.APPROVED);
        if (!schedule.isApprovalRequired()) {
            run.setApprovedBy(createdBy);
            run.setApprovedAt(OffsetDateTime.now());
        }
        PayrollRun savedRun = payrollRunRepository.save(run);

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal deductionTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;

        for (Employee employee : employees) {
            PayrollDtos.EmployeeAdjustmentRequest adjustments = adjustmentMap.getOrDefault(
                employee.getId(),
                new PayrollDtos.EmployeeAdjustmentRequest(employee.getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            );
            EmployeeW4Profile w4Profile = findCurrentW4(employee.getId(), taxYear);
            TaxEngineService.PayrollComputation computation = taxEngineService.calculate(
                employee,
                w4Profile,
                taxYear,
                schedule.getFrequency(),
                adjustments.bonusPay(),
                adjustments.overtimeHours(),
                adjustments.preTaxDeductions()
            );

            PayrollRunItem item = toRunItem(savedRun, employee, computation);
            payrollRunItemRepository.save(item);

            grossTotal = grossTotal.add(computation.grossPay());
            deductionTotal = deductionTotal.add(computation.preTaxDeductions());
            taxTotal = taxTotal.add(computation.employeeTaxTotal());
            netTotal = netTotal.add(computation.netPay());
        }

        savedRun.setGrossTotal(grossTotal);
        savedRun.setDeductionTotal(deductionTotal);
        savedRun.setTaxTotal(taxTotal);
        savedRun.setNetTotal(netTotal);
        schedule.setNextPayDate(schedule.getFrequency().advance(payDate));
        payrollScheduleRepository.save(schedule);
        return payrollRunRepository.save(savedRun);
    }

    @Transactional
    public PayrollRun approveRun(Long runId, String approvedBy) {
        PayrollRun run = payrollRunRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + runId));
        run.setStatus(PayrollRun.RunStatus.APPROVED);
        run.setApprovedBy(approvedBy);
        run.setApprovedAt(OffsetDateTime.now());
        return payrollRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public TaxEngineService.PayrollComputation calculatePayroll(PayrollDtos.PayrollCalculationRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.employeeId()));
        EmployeeW4Profile w4Profile = findCurrentW4(employee.getId(), request.taxYear());
        return taxEngineService.calculate(
            employee,
            w4Profile,
            request.taxYear(),
            request.frequency(),
            request.bonusPay(),
            request.overtimeHours(),
            request.preTaxDeductions()
        );
    }

    private PayrollRunItem toRunItem(
        PayrollRun payrollRun,
        Employee employee,
        TaxEngineService.PayrollComputation computation
    ) {
        PayrollRunItem item = new PayrollRunItem();
        item.setPayrollRun(payrollRun);
        item.setEmployee(employee);
        item.setTaxYear(computation.taxYear());
        item.setGrossPay(computation.grossPay());
        item.setBonusPay(computation.bonusPay());
        item.setOvertimePay(computation.overtimePay());
        item.setPreTaxDeductions(computation.preTaxDeductions());
        item.setTaxableWages(computation.taxableWages());
        item.setFederalIncomeTax(computation.federalIncomeTax());
        item.setSocialSecurityEmployeeTax(computation.socialSecurityEmployeeTax());
        item.setMedicareEmployeeTax(computation.medicareEmployeeTax());
        item.setAdditionalMedicareEmployeeTax(computation.additionalMedicareEmployeeTax());
        item.setStateIncomeTax(computation.stateIncomeTax());
        item.setEmployeeTaxTotal(computation.employeeTaxTotal());
        item.setEmployerSocialSecurityTax(computation.employerSocialSecurityTax());
        item.setEmployerMedicareTax(computation.employerMedicareTax());
        item.setEmployerFutaTax(computation.employerFutaTax());
        item.setNetPay(computation.netPay());
        return item;
    }

    private void validateCompensation(Employee employee) {
        if (employee.getCompensationType() == Employee.CompensationType.HOURLY) {
            if (employee.getHourlyRate() == null || employee.getHourlyRate().signum() <= 0) {
                throw new IllegalArgumentException("Hourly employees must have an hourly rate");
            }
            return;
        }
        if (employee.getAnnualSalary() == null || employee.getAnnualSalary().signum() <= 0) {
            throw new IllegalArgumentException("Salaried employees must have an annual salary");
        }
    }
}
