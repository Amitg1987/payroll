package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.PayrollDtos;
import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import com.embeddedpayroll.backend.model.FederalTaxBracket;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import com.embeddedpayroll.backend.model.UserAccount;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.EmployeeW4ProfileRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.PayrollScheduleRepository;
import com.embeddedpayroll.backend.repository.TaxYearProfileRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final TaxYearProfileRepository taxYearProfileRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeW4ProfileRepository employeeW4ProfileRepository;
    private final PayrollScheduleRepository payrollScheduleRepository;
    private final PayrollService payrollService;
    private final TaxService taxService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            return;
        }

        Organization organization = createOrganization();
        seedTaxProfiles();
        seedUsers(organization);

        Employee john = createEmployee(
            organization,
            "EMP-1001",
            "John",
            "Carter",
            "john.carter@acmepayroll.dev",
            "4821",
            Employee.CompensationType.SALARIED,
            new BigDecimal("125000.00"),
            null,
            new BigDecimal("80.00"),
            new BigDecimal("0.0600"),
            "Finance",
            "CA"
        );
        Employee maria = createEmployee(
            organization,
            "EMP-1002",
            "Maria",
            "Lopez",
            "maria.lopez@acmepayroll.dev",
            "5817",
            Employee.CompensationType.HOURLY,
            null,
            new BigDecimal("42.50"),
            new BigDecimal("80.00"),
            new BigDecimal("0.0325"),
            "Operations",
            "IL"
        );
        Employee kevin = createEmployee(
            organization,
            "EMP-1003",
            "Kevin",
            "Shaw",
            "kevin.shaw@acmepayroll.dev",
            "7004",
            Employee.CompensationType.SALARIED,
            new BigDecimal("88000.00"),
            null,
            new BigDecimal("80.00"),
            new BigDecimal("0.0000"),
            "Engineering",
            "TX"
        );

        seedW4(john, EmployeeW4Profile.FilingStatus.MARRIED_FILING_JOINTLY, new BigDecimal("2000.00"));
        seedW4(maria, EmployeeW4Profile.FilingStatus.HEAD_OF_HOUSEHOLD, new BigDecimal("1000.00"));
        seedW4(kevin, EmployeeW4Profile.FilingStatus.SINGLE, BigDecimal.ZERO);

        PayrollSchedule schedule = new PayrollSchedule();
        schedule.setOrganization(organization);
        schedule.setName("Biweekly Corporate Payroll");
        schedule.setFrequency(PayrollSchedule.PayrollFrequency.BIWEEKLY);
        schedule.setNextPayDate(LocalDate.of(2026, 4, 17));
        schedule.setApprovalRequired(true);
        schedule.setActive(true);
        schedule = payrollScheduleRepository.save(schedule);

        payrollService.approveRun(
            payrollService.processSchedule(
                schedule.getId(),
                new PayrollDtos.ProcessScheduleRequest(LocalDate.of(2025, 12, 31), 2025, List.of()),
                "accountant"
            ).getId(),
            "approver"
        );

        taxService.generateFiling(
            new TaxDtos.GenerateFilingRequest(organization.getId(), 2025, com.embeddedpayroll.backend.model.TaxFilingRecord.FilingType.FORM_W3, "ANNUAL", null)
        );

        payrollService.processSchedule(
            schedule.getId(),
            new PayrollDtos.ProcessScheduleRequest(LocalDate.of(2026, 4, 17), 2026, List.of()),
            "accountant"
        );
    }

    private Organization createOrganization() {
        Organization organization = new Organization();
        organization.setCode("ACME-PAY-US");
        organization.setLegalName("Acme Payroll Services Inc.");
        organization.setDbaName("Acme Embedded Payroll");
        organization.setEin("12-3456789");
        organization.setDefaultStateCode("CA");
        organization.setDefaultCurrency("USD");
        organization.setAccountingMethod("ACCRUAL");
        organization.setContactEmail("payroll-ops@acmepayroll.dev");
        return organizationRepository.save(organization);
    }

    private void seedUsers(Organization organization) {
        userAccountRepository.save(createUser(
            organization,
            "admin",
            "Admin@123",
            "Payroll Platform Admin",
            "admin@acmepayroll.dev",
            UserAccount.RoleName.ADMIN
        ));
        userAccountRepository.save(createUser(
            organization,
            "accountant",
            "Accountant@123",
            "Corporate Accountant",
            "accountant@acmepayroll.dev",
            UserAccount.RoleName.ACCOUNTANT
        ));
        userAccountRepository.save(createUser(
            organization,
            "approver",
            "Approver@123",
            "Payroll Approver",
            "approver@acmepayroll.dev",
            UserAccount.RoleName.APPROVER
        ));
    }

    private UserAccount createUser(
        Organization organization,
        String username,
        String rawPassword,
        String fullName,
        String email,
        UserAccount.RoleName role
    ) {
        UserAccount userAccount = new UserAccount();
        userAccount.setOrganization(organization);
        userAccount.setUsername(username);
        userAccount.setPasswordHash(passwordEncoder.encode(rawPassword));
        userAccount.setFullName(fullName);
        userAccount.setEmail(email);
        userAccount.getRoles().add(role);
        return userAccount;
    }

    private Employee createEmployee(
        Organization organization,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String ssnLastFour,
        Employee.CompensationType compensationType,
        BigDecimal annualSalary,
        BigDecimal hourlyRate,
        BigDecimal standardHoursPerPeriod,
        BigDecimal stateWithholdingRate,
        String department,
        String workState
    ) {
        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setEmployeeNumber(employeeNumber);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setSsnLastFour(ssnLastFour);
        employee.setHireDate(LocalDate.of(2021, 1, 4));
        employee.setEmploymentStatus(Employee.EmploymentStatus.ACTIVE);
        employee.setCompensationType(compensationType);
        employee.setAnnualSalary(annualSalary);
        employee.setHourlyRate(hourlyRate);
        employee.setStandardHoursPerPeriod(standardHoursPerPeriod);
        employee.setStateWithholdingRate(stateWithholdingRate);
        employee.setDepartment(department);
        employee.setWorkState(workState);
        return employeeRepository.save(employee);
    }

    private void seedW4(Employee employee, EmployeeW4Profile.FilingStatus filingStatus, BigDecimal dependentsCredit) {
        for (int taxYear = 2025; taxYear <= 2026; taxYear++) {
            EmployeeW4Profile profile = new EmployeeW4Profile();
            profile.setEmployee(employee);
            profile.setTaxYear(taxYear);
            profile.setFilingStatus(filingStatus);
            profile.setMultipleJobs(false);
            profile.setDependentsCredit(dependentsCredit);
            profile.setOtherIncome(BigDecimal.ZERO);
            profile.setDeductions(BigDecimal.ZERO);
            profile.setExtraWithholding(BigDecimal.ZERO);
            profile.setExemptFromWithholding(false);
            employeeW4ProfileRepository.save(profile);
        }
    }

    private void seedTaxProfiles() {
        List<TaxProfileSeed> seeds = List.of(
            new TaxProfileSeed(2021, 1.00, "2021 historical payroll profile"),
            new TaxProfileSeed(2022, 1.03, "2022 historical payroll profile"),
            new TaxProfileSeed(2023, 1.08, "2023 historical payroll profile"),
            new TaxProfileSeed(2024, 1.12, "2024 historical payroll profile"),
            new TaxProfileSeed(2025, 1.16, "2025 historical payroll profile"),
            new TaxProfileSeed(2026, 1.20, "2026 provisional payroll profile")
        );

        for (TaxProfileSeed seed : seeds) {
            TaxYearProfile profile = new TaxYearProfile();
            profile.setTaxYear(seed.taxYear());
            profile.setSocialSecurityEmployeeRate(new BigDecimal("0.0620"));
            profile.setSocialSecurityEmployerRate(new BigDecimal("0.0620"));
            profile.setSocialSecurityWageBase(switch (seed.taxYear()) {
                case 2021 -> new BigDecimal("142800.00");
                case 2022 -> new BigDecimal("147000.00");
                case 2023 -> new BigDecimal("160200.00");
                case 2024 -> new BigDecimal("168600.00");
                case 2025 -> new BigDecimal("176100.00");
                default -> new BigDecimal("182000.00");
            });
            profile.setMedicareEmployeeRate(new BigDecimal("0.0145"));
            profile.setMedicareEmployerRate(new BigDecimal("0.0145"));
            profile.setAdditionalMedicareRate(new BigDecimal("0.0090"));
            profile.setAdditionalMedicareThreshold(new BigDecimal("200000.00"));
            profile.setFederalUnemploymentRate(new BigDecimal("0.0060"));
            profile.setFederalUnemploymentWageBase(new BigDecimal("7000.00"));
            profile.setDefaultStateUnemploymentRate(new BigDecimal("0.0270"));
            profile.setStandardDeductionSingle(switch (seed.taxYear()) {
                case 2021 -> new BigDecimal("12550.00");
                case 2022 -> new BigDecimal("12950.00");
                case 2023 -> new BigDecimal("13850.00");
                case 2024 -> new BigDecimal("14600.00");
                case 2025 -> new BigDecimal("15000.00");
                default -> new BigDecimal("15300.00");
            });
            profile.setStandardDeductionMarriedJointly(switch (seed.taxYear()) {
                case 2021 -> new BigDecimal("25100.00");
                case 2022 -> new BigDecimal("25900.00");
                case 2023 -> new BigDecimal("27700.00");
                case 2024 -> new BigDecimal("29200.00");
                case 2025 -> new BigDecimal("30000.00");
                default -> new BigDecimal("30600.00");
            });
            profile.setStandardDeductionHeadOfHousehold(switch (seed.taxYear()) {
                case 2021 -> new BigDecimal("18800.00");
                case 2022 -> new BigDecimal("19400.00");
                case 2023 -> new BigDecimal("20800.00");
                case 2024 -> new BigDecimal("21900.00");
                case 2025 -> new BigDecimal("22500.00");
                default -> new BigDecimal("23000.00");
            });
            profile.setNotes(seed.notes() + " seeded for embedded payroll demos and historical calculations.");

            addBrackets(profile, EmployeeW4Profile.FilingStatus.SINGLE, seed.multiplier(), List.of(
                9950d, 40525d, 86375d, 164925d, 209425d, 523600d
            ));
            addBrackets(profile, EmployeeW4Profile.FilingStatus.MARRIED_FILING_JOINTLY, seed.multiplier(), List.of(
                19900d, 81050d, 172750d, 329850d, 418850d, 628300d
            ));
            addBrackets(profile, EmployeeW4Profile.FilingStatus.HEAD_OF_HOUSEHOLD, seed.multiplier(), List.of(
                14200d, 54200d, 86350d, 164900d, 209400d, 523600d
            ));
            taxYearProfileRepository.save(profile);
        }
    }

    private void addBrackets(
        TaxYearProfile profile,
        EmployeeW4Profile.FilingStatus filingStatus,
        double multiplier,
        List<Double> thresholds
    ) {
        List<BigDecimal> rates = List.of(
            new BigDecimal("0.10"),
            new BigDecimal("0.12"),
            new BigDecimal("0.22"),
            new BigDecimal("0.24"),
            new BigDecimal("0.32"),
            new BigDecimal("0.35"),
            new BigDecimal("0.37")
        );

        BigDecimal lower = BigDecimal.ZERO;
        for (int i = 0; i < thresholds.size(); i++) {
            FederalTaxBracket bracket = new FederalTaxBracket();
            bracket.setFilingStatus(filingStatus);
            bracket.setBracketOrder(i + 1);
            bracket.setLowerBound(lower);
            BigDecimal upper = BigDecimal.valueOf(thresholds.get(i) * multiplier).setScale(2, RoundingMode.HALF_UP);
            bracket.setUpperBound(upper);
            bracket.setRate(rates.get(i));
            profile.addBracket(bracket);
            lower = upper;
        }
        FederalTaxBracket topBracket = new FederalTaxBracket();
        topBracket.setFilingStatus(filingStatus);
        topBracket.setBracketOrder(thresholds.size() + 1);
        topBracket.setLowerBound(lower);
        topBracket.setUpperBound(null);
        topBracket.setRate(rates.get(rates.size() - 1));
        profile.addBracket(topBracket);
    }

    private record TaxProfileSeed(int taxYear, double multiplier, String notes) {
    }
}
