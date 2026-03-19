package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByTenantKey(String tenantKey);
}
