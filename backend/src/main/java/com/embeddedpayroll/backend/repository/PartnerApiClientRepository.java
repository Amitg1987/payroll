package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.PartnerApiClient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerApiClientRepository extends JpaRepository<PartnerApiClient, Long> {

    List<PartnerApiClient> findByOrganizationIdOrderByDisplayNameAsc(Long organizationId);

    Optional<PartnerApiClient> findByClientCode(String clientCode);

    Optional<PartnerApiClient> findByKeyPrefix(String keyPrefix);

    List<PartnerApiClient> findByActiveTrue();
}
