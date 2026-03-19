package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "federal_tax_brackets")
public class FederalTaxBracket extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "tax_year_profile_id", nullable = false)
    private TaxYearProfile taxYearProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmployeeW4Profile.FilingStatus filingStatus;

    @Column(name = "bracket_order", nullable = false)
    private Integer bracketOrder;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lowerBound;

    @Column(precision = 14, scale = 2)
    private BigDecimal upperBound;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal rate;
}
