package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.StateReciprocityAgreement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateReciprocityAgreementRepository extends JpaRepository<StateReciprocityAgreement, Long> {

    Optional<StateReciprocityAgreement> findByResidentStateCodeAndWorkStateCodeAndActiveTrue(
        String residentStateCode,
        String workStateCode
    );
}
