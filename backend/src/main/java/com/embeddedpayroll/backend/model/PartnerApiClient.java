package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "partner_api_clients")
public class PartnerApiClient extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, length = 80)
    private String clientCode;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, unique = true, length = 32)
    private String keyPrefix;

    @Column(nullable = false, length = 128)
    private String apiKeyHash;

    @Column(nullable = false, length = 240)
    private String scopes;

    @Column(length = 240)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
