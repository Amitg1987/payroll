package com.embeddedpayroll.backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tax_year_profiles")
public class TaxYearProfile extends BaseEntity {

    @Column(name = "tax_year", nullable = false, unique = true)
    private Integer taxYear;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal socialSecurityEmployeeRate;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal socialSecurityEmployerRate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal socialSecurityWageBase;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal medicareEmployeeRate;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal medicareEmployerRate;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal additionalMedicareRate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal additionalMedicareThreshold;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal federalUnemploymentRate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal federalUnemploymentWageBase;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal defaultStateUnemploymentRate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal standardDeductionSingle;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal standardDeductionMarriedJointly;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal standardDeductionHeadOfHousehold;

    @Column(nullable = false, length = 240)
    private String notes;

    @OneToMany(mappedBy = "taxYearProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FederalTaxBracket> federalTaxBrackets = new ArrayList<>();

    public void addBracket(FederalTaxBracket bracket) {
        bracket.setTaxYearProfile(this);
        federalTaxBrackets.add(bracket);
        federalTaxBrackets.sort(Comparator.comparing(FederalTaxBracket::getBracketOrder));
    }
}
