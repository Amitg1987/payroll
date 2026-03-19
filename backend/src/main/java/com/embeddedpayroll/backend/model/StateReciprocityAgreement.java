package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "state_reciprocity_agreements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_state_reciprocity",
        columnNames = {"resident_state_code", "work_state_code"}
    )
)
public class StateReciprocityAgreement extends BaseEntity {

    @Column(name = "resident_state_code", nullable = false, length = 8)
    private String residentStateCode;

    @Column(name = "work_state_code", nullable = false, length = 8)
    private String workStateCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 240)
    private String notes;
}
