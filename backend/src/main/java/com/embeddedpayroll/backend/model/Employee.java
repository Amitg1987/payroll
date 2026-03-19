package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "employees",
    uniqueConstraints = @UniqueConstraint(name = "uk_employee_org_number", columnNames = {"organization_id", "employee_number"})
)
public class Employee extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "employee_number", nullable = false, length = 32)
    private String employeeNumber;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = 4)
    private String ssnLastFour;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CompensationType compensationType = CompensationType.SALARIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WorkerType workerType = WorkerType.W2_EMPLOYEE;

    @Column(length = 80)
    private String department;

    @Column(nullable = false, length = 8)
    private String workState;

    @Column(nullable = false, length = 8)
    private String residenceState;

    @Column(length = 32)
    private String workLocalJurisdictionCode;

    @Column(length = 32)
    private String residenceLocalJurisdictionCode;

    @Column(precision = 14, scale = 2)
    private BigDecimal annualSalary;

    @Column(precision = 14, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal standardHoursPerPeriod = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal stateWithholdingRate = BigDecimal.ZERO;

    public enum EmploymentStatus {
        ACTIVE,
        ON_LEAVE,
        TERMINATED
    }

    public enum CompensationType {
        SALARIED,
        HOURLY
    }

    public enum WorkerType {
        W2_EMPLOYEE
    }
}
