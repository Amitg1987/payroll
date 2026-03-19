package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.PayrollDtos;
import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import com.embeddedpayroll.backend.model.FederalTaxBracket;
import com.embeddedpayroll.backend.model.JurisdictionTaxProfile;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.OrganizationJurisdiction;
import com.embeddedpayroll.backend.model.PartnerApiClient;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.model.TaxJurisdiction;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import com.embeddedpayroll.backend.model.UserAccount;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.EmployeeW4ProfileRepository;
import com.embeddedpayroll.backend.repository.JurisdictionTaxProfileRepository;
import com.embeddedpayroll.backend.repository.OrganizationJurisdictionRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.PartnerApiClientRepository;
import com.embeddedpayroll.backend.repository.PayrollScheduleRepository;
import com.embeddedpayroll.backend.repository.TaxJurisdictionRepository;
import com.embeddedpayroll.backend.repository.TaxYearProfileRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import com.embeddedpayroll.backend.security.ApiKeyHashService;
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

    public static final String DEMO_PARTNER_API_KEY = "pk_live_demo_embedded_payroll_partner_2026";

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final TaxYearProfileRepository taxYearProfileRepository;
    private final TaxJurisdictionRepository taxJurisdictionRepository;
    private final JurisdictionTaxProfileRepository jurisdictionTaxProfileRepository;
    private final OrganizationJurisdictionRepository organizationJurisdictionRepository;
    private final PartnerApiClientRepository partnerApiClientRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeW4ProfileRepository employeeW4ProfileRepository;
    private final PayrollScheduleRepository payrollScheduleRepository;
    private final PayrollService payrollService;
    private final TaxFilingWorkflowService taxFilingWorkflowService;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyHashService apiKeyHashService;

    @Override
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            return;
        }

        seedTaxJurisdictions();
        seedFederalTaxProfiles();
        seedJurisdictionTaxProfiles();

        Organization acme = createOrganization(
            "acme-pay-us",
            "ACME-PAY-US",
            "Acme Payroll Services Inc.",
            "Acme Embedded Payroll",
            "12-3456789",
            "CA",
            "CA",
            "SF_CA",
            "payroll-ops@acmepayroll.dev"
        );
        Organization northwind = createOrganization(
            "northwind-manufacturing",
            "NORTHWIND-US",
            "Northwind Manufacturing LLC",
            "Northwind Workforce Services",
            "98-7654321",
            "NY",
            "NY",
            "NYC_NY",
            "payroll@northwind.dev"
        );

        seedOrganizationJurisdictions(acme, List.of("CA", "IL", "TX", "SF_CA"));
        seedOrganizationJurisdictions(northwind, List.of("NY", "NYC_NY"));

        seedUsers(acme, "admin", "accountant", "approver", "integrator");
        seedUsers(northwind, "northwind-admin", "northwind-accountant", "northwind-approver", "northwind-integrator");
        seedPartnerClient(acme, "acme-platform-sdk", "Acme Embedded Payroll SDK", DEMO_PARTNER_API_KEY);
        seedPartnerClient(northwind, "northwind-embedded-payroll", "Northwind Embedded Payroll", "pk_live_northwind_partner_2026");

        Employee john = createEmployee(
            acme,
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
            "CA",
            "CA",
            "SF_CA",
            "SF_CA"
        );
        Employee maria = createEmployee(
            acme,
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
            "IL",
            "IL",
            "CHI_IL",
            "CHI_IL"
        );
        Employee kevin = createEmployee(
            acme,
            "EMP-1003",
            "Kevin",
            "Shaw",
            "kevin.shaw@acmepayroll.dev",
            "7004",
            Employee.CompensationType.SALARIED,
            new BigDecimal("88000.00"),
            null,
            new BigDecimal("80.00"),
            BigDecimal.ZERO,
            "Engineering",
            "TX",
            "TX",
            null,
            null
        );

        seedW4(john, EmployeeW4Profile.FilingStatus.MARRIED_FILING_JOINTLY, new BigDecimal("2000.00"));
        seedW4(maria, EmployeeW4Profile.FilingStatus.HEAD_OF_HOUSEHOLD, new BigDecimal("1000.00"));
        seedW4(kevin, EmployeeW4Profile.FilingStatus.SINGLE, BigDecimal.ZERO);

        PayrollSchedule schedule = new PayrollSchedule();
        schedule.setOrganization(acme);
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

        taxFilingWorkflowService.startWorkflow(
            new TaxDtos.StartFilingWorkflowRequest(
                acme.getId(),
                2025,
                "ANNUAL",
                List.of(TaxFilingRecord.FilingType.FORM_W2, TaxFilingRecord.FilingType.FORM_W3)
            ),
            "integrator"
        );

        payrollService.processSchedule(
            schedule.getId(),
            new PayrollDtos.ProcessScheduleRequest(LocalDate.of(2026, 4, 17), 2026, List.of()),
            "accountant"
        );
    }

    private Organization createOrganization(
        String tenantKey,
        String code,
        String legalName,
        String dbaName,
        String ein,
        String defaultStateCode,
        String primaryJurisdictionCode,
        String headquartersLocalJurisdictionCode,
        String contactEmail
    ) {
        Organization organization = new Organization();
        organization.setTenantKey(tenantKey);
        organization.setCode(code);
        organization.setLegalName(legalName);
        organization.setDbaName(dbaName);
        organization.setEin(ein);
        organization.setDefaultStateCode(defaultStateCode);
        organization.setCountryCode("US");
        organization.setPrimaryJurisdictionCode(primaryJurisdictionCode);
        organization.setHeadquartersLocalJurisdictionCode(headquartersLocalJurisdictionCode);
        organization.setDefaultCurrency("USD");
        organization.setAccountingMethod("ACCRUAL");
        organization.setContactEmail(contactEmail);
        return organizationRepository.save(organization);
    }

    private void seedOrganizationJurisdictions(Organization organization, List<String> jurisdictionCodes) {
        for (String jurisdictionCode : jurisdictionCodes) {
            TaxJurisdiction jurisdiction = taxJurisdictionRepository.findByCode(jurisdictionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown jurisdiction seed: " + jurisdictionCode));
            OrganizationJurisdiction organizationJurisdiction = new OrganizationJurisdiction();
            organizationJurisdiction.setOrganization(organization);
            organizationJurisdiction.setTaxJurisdiction(jurisdiction);
            organizationJurisdiction.setNexusType(
                jurisdiction.getJurisdictionType() == TaxJurisdiction.JurisdictionType.LOCAL
                    ? OrganizationJurisdiction.NexusType.WORK_LOCATION
                    : OrganizationJurisdiction.NexusType.BOTH
            );
            organizationJurisdiction.setRegistrationNumber(organization.getCode() + "-" + jurisdictionCode + "-REG");
            organizationJurisdiction.setEmployerAccountNumber(organization.getCode() + "-" + jurisdictionCode + "-ACCT");
            organizationJurisdiction.setPrimaryJurisdiction(jurisdictionCode.equals(organization.getPrimaryJurisdictionCode()));
            organizationJurisdictionRepository.save(organizationJurisdiction);
        }
    }

    private void seedUsers(
        Organization organization,
        String adminUsername,
        String accountantUsername,
        String approverUsername,
        String integratorUsername
    ) {
        userAccountRepository.save(createUser(
            organization,
            adminUsername,
            "Admin@123",
            "Payroll Platform Admin",
            adminUsername + "@acmepayroll.dev",
            UserAccount.RoleName.ADMIN
        ));
        userAccountRepository.save(createUser(
            organization,
            accountantUsername,
            "Accountant@123",
            "Corporate Accountant",
            accountantUsername + "@acmepayroll.dev",
            UserAccount.RoleName.ACCOUNTANT
        ));
        userAccountRepository.save(createUser(
            organization,
            approverUsername,
            "Approver@123",
            "Payroll Approver",
            approverUsername + "@acmepayroll.dev",
            UserAccount.RoleName.APPROVER
        ));
        userAccountRepository.save(createUser(
            organization,
            integratorUsername,
            "Integrator@123",
            "Developer / Platform Integrator",
            integratorUsername + "@acmepayroll.dev",
            UserAccount.RoleName.DEVELOPER_PLATFORM_INTEGRATOR
        ));
    }

    private void seedPartnerClient(
        Organization organization,
        String clientCode,
        String displayName,
        String rawApiKey
    ) {
        PartnerApiClient apiClient = new PartnerApiClient();
        apiClient.setOrganization(organization);
        apiClient.setClientCode(clientCode);
        apiClient.setDisplayName(displayName);
        apiClient.setDescription("Programmatic payroll integration client for embedded platform usage");
        apiClient.setScopes("employees:write,payroll:write,reports:read,webhooks:manage,tax:write");
        apiClient.setKeyPrefix(rawApiKey.substring(0, 12));
        apiClient.setApiKeyHash(apiKeyHashService.hash(rawApiKey));
        apiClient.setActive(true);
        partnerApiClientRepository.save(apiClient);
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
        String workState,
        String residenceState,
        String workLocalJurisdictionCode,
        String residenceLocalJurisdictionCode
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
        employee.setWorkerType(Employee.WorkerType.W2_EMPLOYEE);
        employee.setAnnualSalary(annualSalary);
        employee.setHourlyRate(hourlyRate);
        employee.setStandardHoursPerPeriod(standardHoursPerPeriod);
        employee.setStateWithholdingRate(stateWithholdingRate);
        employee.setDepartment(department);
        employee.setWorkState(workState);
        employee.setResidenceState(residenceState);
        employee.setWorkLocalJurisdictionCode(workLocalJurisdictionCode);
        employee.setResidenceLocalJurisdictionCode(residenceLocalJurisdictionCode);
        return employeeRepository.save(employee);
    }

    private void seedW4(Employee employee, EmployeeW4Profile.FilingStatus filingStatus, BigDecimal dependentsCredit) {
        for (int taxYear = 2021; taxYear <= 2026; taxYear++) {
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

    private void seedFederalTaxProfiles() {
        List<TaxProfileSeed> seeds = List.of(
            new TaxProfileSeed(2021, 1.00, "2021 historical federal payroll profile"),
            new TaxProfileSeed(2022, 1.03, "2022 historical federal payroll profile"),
            new TaxProfileSeed(2023, 1.08, "2023 historical federal payroll profile"),
            new TaxProfileSeed(2024, 1.12, "2024 historical federal payroll profile"),
            new TaxProfileSeed(2025, 1.16, "2025 historical federal payroll profile"),
            new TaxProfileSeed(2026, 1.20, "2026 provisional federal payroll profile")
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
            profile.setNotes(seed.notes() + " seeded for multi-tenant embedded payroll demos and historical calculations.");

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

    private void seedTaxJurisdictions() {
        createJurisdiction("US", "United States Federal", TaxJurisdiction.JurisdictionType.FEDERAL, null, null);
        for (StateSeed stateSeed : stateSeeds()) {
            createJurisdiction(stateSeed.code(), stateSeed.name(), TaxJurisdiction.JurisdictionType.STATE, stateSeed.code(), "US");
        }
        for (LocalSeed localSeed : localSeeds()) {
            createJurisdiction(
                localSeed.code(),
                localSeed.name(),
                TaxJurisdiction.JurisdictionType.LOCAL,
                localSeed.stateCode(),
                localSeed.parentJurisdictionCode()
            );
        }
    }

    private void createJurisdiction(
        String code,
        String name,
        TaxJurisdiction.JurisdictionType jurisdictionType,
        String stateCode,
        String parentJurisdictionCode
    ) {
        TaxJurisdiction jurisdiction = new TaxJurisdiction();
        jurisdiction.setCode(code);
        jurisdiction.setName(name);
        jurisdiction.setJurisdictionType(jurisdictionType);
        jurisdiction.setCountryCode("US");
        jurisdiction.setStateCode(stateCode);
        jurisdiction.setParentJurisdictionCode(parentJurisdictionCode);
        jurisdiction.setActive(true);
        taxJurisdictionRepository.save(jurisdiction);
    }

    private void seedJurisdictionTaxProfiles() {
        for (int taxYear = 2021; taxYear <= 2026; taxYear++) {
            for (StateSeed stateSeed : stateSeeds()) {
                TaxJurisdiction jurisdiction = taxJurisdictionRepository.findByCode(stateSeed.code()).orElseThrow();
                createJurisdictionProfile(
                    jurisdiction,
                    taxYear,
                    JurisdictionTaxProfile.TaxType.STATE_WITHHOLDING,
                    stateSeed.withholdingRate(),
                    stateSeed.withholdingRate(),
                    BigDecimal.ZERO,
                    null,
                    stateSeed.standardDeduction(),
                    stateSeed.name() + " resident/nonresident state withholding seed"
                );
                createJurisdictionProfile(
                    jurisdiction,
                    taxYear,
                    JurisdictionTaxProfile.TaxType.STATE_UNEMPLOYMENT,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    stateSeed.stateUnemploymentRate(),
                    new BigDecimal("12000.00"),
                    BigDecimal.ZERO,
                    stateSeed.name() + " SUTA employer rate seed"
                );
            }

            for (LocalSeed localSeed : localSeeds()) {
                TaxJurisdiction jurisdiction = taxJurisdictionRepository.findByCode(localSeed.code()).orElseThrow();
                createJurisdictionProfile(
                    jurisdiction,
                    taxYear,
                    JurisdictionTaxProfile.TaxType.LOCAL_WITHHOLDING,
                    localSeed.rate(),
                    localSeed.nonResidentRate(),
                    BigDecimal.ZERO,
                    null,
                    BigDecimal.ZERO,
                    localSeed.name() + " local withholding seed"
                );
            }
        }
    }

    private void createJurisdictionProfile(
        TaxJurisdiction jurisdiction,
        Integer taxYear,
        JurisdictionTaxProfile.TaxType taxType,
        BigDecimal residentRate,
        BigDecimal nonResidentRate,
        BigDecimal employerRate,
        BigDecimal wageBase,
        BigDecimal standardDeduction,
        String notes
    ) {
        JurisdictionTaxProfile profile = new JurisdictionTaxProfile();
        profile.setTaxJurisdiction(jurisdiction);
        profile.setTaxYear(taxYear);
        profile.setTaxType(taxType);
        profile.setResidentRate(residentRate);
        profile.setNonResidentRate(nonResidentRate);
        profile.setEmployerRate(employerRate);
        profile.setWageBase(wageBase);
        profile.setStandardDeduction(standardDeduction);
        profile.setNotes(notes);
        jurisdictionTaxProfileRepository.save(profile);
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
        for (int index = 0; index < thresholds.size(); index++) {
            FederalTaxBracket bracket = new FederalTaxBracket();
            bracket.setFilingStatus(filingStatus);
            bracket.setBracketOrder(index + 1);
            bracket.setLowerBound(lower);
            BigDecimal upper = BigDecimal.valueOf(thresholds.get(index) * multiplier).setScale(2, RoundingMode.HALF_UP);
            bracket.setUpperBound(upper);
            bracket.setRate(rates.get(index));
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

    private List<StateSeed> stateSeeds() {
        return List.of(
            new StateSeed("AL", "Alabama", bd("0.0450"), bd("0.0270"), bd("2500.00")),
            new StateSeed("AK", "Alaska", bd("0.0000"), bd("0.0180"), BigDecimal.ZERO),
            new StateSeed("AZ", "Arizona", bd("0.0250"), bd("0.0240"), BigDecimal.ZERO),
            new StateSeed("AR", "Arkansas", bd("0.0470"), bd("0.0280"), bd("2200.00")),
            new StateSeed("CA", "California", bd("0.0600"), bd("0.0340"), bd("5202.00")),
            new StateSeed("CO", "Colorado", bd("0.0440"), bd("0.0200"), BigDecimal.ZERO),
            new StateSeed("CT", "Connecticut", bd("0.0500"), bd("0.0280"), bd("15000.00")),
            new StateSeed("DE", "Delaware", bd("0.0480"), bd("0.0210"), bd("3250.00")),
            new StateSeed("FL", "Florida", bd("0.0000"), bd("0.0270"), BigDecimal.ZERO),
            new StateSeed("GA", "Georgia", bd("0.0540"), bd("0.0260"), bd("5400.00")),
            new StateSeed("HI", "Hawaii", bd("0.0580"), bd("0.0300"), bd("2200.00")),
            new StateSeed("ID", "Idaho", bd("0.0580"), bd("0.0120"), BigDecimal.ZERO),
            new StateSeed("IL", "Illinois", bd("0.0325"), bd("0.0270"), BigDecimal.ZERO),
            new StateSeed("IN", "Indiana", bd("0.0315"), bd("0.0220"), BigDecimal.ZERO),
            new StateSeed("IA", "Iowa", bd("0.0440"), bd("0.0120"), bd("2210.00")),
            new StateSeed("KS", "Kansas", bd("0.0450"), bd("0.0230"), BigDecimal.ZERO),
            new StateSeed("KY", "Kentucky", bd("0.0400"), bd("0.0270"), BigDecimal.ZERO),
            new StateSeed("LA", "Louisiana", bd("0.0420"), bd("0.0180"), bd("4500.00")),
            new StateSeed("ME", "Maine", bd("0.0520"), bd("0.0240"), bd("13450.00")),
            new StateSeed("MD", "Maryland", bd("0.0475"), bd("0.0260"), bd("2350.00")),
            new StateSeed("MA", "Massachusetts", bd("0.0500"), bd("0.0240"), BigDecimal.ZERO),
            new StateSeed("MI", "Michigan", bd("0.0425"), bd("0.0260"), BigDecimal.ZERO),
            new StateSeed("MN", "Minnesota", bd("0.0540"), bd("0.0240"), bd("14575.00")),
            new StateSeed("MS", "Mississippi", bd("0.0400"), bd("0.0200"), BigDecimal.ZERO),
            new StateSeed("MO", "Missouri", bd("0.0450"), bd("0.0260"), bd("14000.00")),
            new StateSeed("MT", "Montana", bd("0.0510"), bd("0.0240"), bd("5750.00")),
            new StateSeed("NE", "Nebraska", bd("0.0510"), bd("0.0200"), bd("7350.00")),
            new StateSeed("NV", "Nevada", bd("0.0000"), bd("0.0250"), BigDecimal.ZERO),
            new StateSeed("NH", "New Hampshire", bd("0.0000"), bd("0.0170"), BigDecimal.ZERO),
            new StateSeed("NJ", "New Jersey", bd("0.0530"), bd("0.0270"), bd("1000.00")),
            new StateSeed("NM", "New Mexico", bd("0.0430"), bd("0.0240"), bd("12550.00")),
            new StateSeed("NY", "New York", bd("0.0600"), bd("0.0310"), bd("8000.00")),
            new StateSeed("NC", "North Carolina", bd("0.0475"), bd("0.0120"), BigDecimal.ZERO),
            new StateSeed("ND", "North Dakota", bd("0.0280"), bd("0.0120"), BigDecimal.ZERO),
            new StateSeed("OH", "Ohio", bd("0.0380"), bd("0.0270"), bd("26000.00")),
            new StateSeed("OK", "Oklahoma", bd("0.0430"), bd("0.0200"), bd("6350.00")),
            new StateSeed("OR", "Oregon", bd("0.0570"), bd("0.0290"), bd("2605.00")),
            new StateSeed("PA", "Pennsylvania", bd("0.0307"), bd("0.0280"), BigDecimal.ZERO),
            new StateSeed("RI", "Rhode Island", bd("0.0475"), bd("0.0220"), bd("10000.00")),
            new StateSeed("SC", "South Carolina", bd("0.0430"), bd("0.0210"), bd("14600.00")),
            new StateSeed("SD", "South Dakota", bd("0.0000"), bd("0.0120"), BigDecimal.ZERO),
            new StateSeed("TN", "Tennessee", bd("0.0000"), bd("0.0180"), BigDecimal.ZERO),
            new StateSeed("TX", "Texas", bd("0.0000"), bd("0.0160"), BigDecimal.ZERO),
            new StateSeed("UT", "Utah", bd("0.0485"), bd("0.0230"), BigDecimal.ZERO),
            new StateSeed("VT", "Vermont", bd("0.0550"), bd("0.0180"), bd("6500.00")),
            new StateSeed("VA", "Virginia", bd("0.0450"), bd("0.0210"), bd("8000.00")),
            new StateSeed("WA", "Washington", bd("0.0000"), bd("0.0260"), BigDecimal.ZERO),
            new StateSeed("WV", "West Virginia", bd("0.0440"), bd("0.0250"), bd("2000.00")),
            new StateSeed("WI", "Wisconsin", bd("0.0465"), bd("0.0240"), bd("12760.00")),
            new StateSeed("WY", "Wyoming", bd("0.0000"), bd("0.0180"), BigDecimal.ZERO)
        );
    }

    private List<LocalSeed> localSeeds() {
        return List.of(
            new LocalSeed("SF_CA", "San Francisco, CA Payroll Tax", "CA", "CA", bd("0.0060"), bd("0.0030")),
            new LocalSeed("CHI_IL", "Chicago, IL Local Withholding", "IL", "IL", bd("0.0125"), bd("0.0060")),
            new LocalSeed("NYC_NY", "New York City, NY Local Tax", "NY", "NY", bd("0.0320"), bd("0.0100")),
            new LocalSeed("PHL_PA", "Philadelphia, PA Wage Tax", "PA", "PA", bd("0.0375"), bd("0.0344")),
            new LocalSeed("DENVER_CO", "Denver, CO Occupational Privilege", "CO", "CO", bd("0.0040"), bd("0.0020")),
            new LocalSeed("PORTLAND_OR", "Portland, OR Metro Tax", "OR", "OR", bd("0.0100"), bd("0.0060"))
        );
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record TaxProfileSeed(int taxYear, double multiplier, String notes) {
    }

    private record StateSeed(
        String code,
        String name,
        BigDecimal withholdingRate,
        BigDecimal stateUnemploymentRate,
        BigDecimal standardDeduction
    ) {
    }

    private record LocalSeed(
        String code,
        String name,
        String stateCode,
        String parentJurisdictionCode,
        BigDecimal rate,
        BigDecimal nonResidentRate
    ) {
    }
}
