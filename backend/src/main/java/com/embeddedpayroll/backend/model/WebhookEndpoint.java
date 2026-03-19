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
@Table(name = "webhook_endpoints")
public class WebhookEndpoint extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "partner_api_client_id")
    private PartnerApiClient partnerApiClient;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 320)
    private String targetUrl;

    @Column(nullable = false, length = 120)
    private String subscribedEvent;

    @Column(nullable = false, length = 120)
    private String signingSecret;

    @Column(nullable = false)
    private boolean active = true;
}
