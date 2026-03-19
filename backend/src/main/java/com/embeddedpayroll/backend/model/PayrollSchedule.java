package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_schedules")
public class PayrollSchedule extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PayrollFrequency frequency;

    @Column(nullable = false)
    private LocalDate nextPayDate;

    @Column(nullable = false)
    private boolean approvalRequired = true;

    @Column(nullable = false)
    private boolean active = true;

    public enum PayrollFrequency {
        WEEKLY(52, 7),
        BIWEEKLY(26, 14),
        SEMIMONTHLY(24, 15),
        MONTHLY(12, 30);

        private final int periodsPerYear;
        private final int nominalDays;

        PayrollFrequency(int periodsPerYear, int nominalDays) {
            this.periodsPerYear = periodsPerYear;
            this.nominalDays = nominalDays;
        }

        public int periodsPerYear() {
            return periodsPerYear;
        }

        public int nominalDays() {
            return nominalDays;
        }

        public LocalDate advance(LocalDate date) {
            return switch (this) {
                case WEEKLY -> date.plusWeeks(1);
                case BIWEEKLY -> date.plusWeeks(2);
                case SEMIMONTHLY -> date.plusDays(15);
                case MONTHLY -> date.plusMonths(1);
            };
        }
    }
}
