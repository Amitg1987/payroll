package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.IdempotencyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByOrganizationIdAndActorKeyAndIdempotencyKey(
        Long organizationId,
        String actorKey,
        String idempotencyKey
    );
}
