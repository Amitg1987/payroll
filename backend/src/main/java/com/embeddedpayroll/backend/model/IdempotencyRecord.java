package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
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
    name = "idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_actor_key",
        columnNames = {"organization_id", "actor_key", "idempotency_key"}
    )
)
public class IdempotencyRecord extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "actor_key", nullable = false, length = 120)
    private String actorKey;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, length = 16)
    private String requestMethod;

    @Column(nullable = false, length = 240)
    private String requestPath;

    @Column(nullable = false, length = 128)
    private String requestHash;

    @Column(nullable = false)
    private Integer responseStatusCode;

    @Lob
    @Column(nullable = false)
    private String responseBody;

    @Column(nullable = false, length = 120)
    private String responseType;
}
