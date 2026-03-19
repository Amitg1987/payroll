package com.embeddedpayroll.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "webhook_endpoint_id", nullable = false)
    private WebhookEndpoint webhookEndpoint;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String eventKey;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(nullable = false, length = 160)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(nullable = false)
    private Integer attempts = 0;

    private OffsetDateTime nextAttemptAt = OffsetDateTime.now();

    private OffsetDateTime deliveredAt;

    private Integer responseStatusCode;

    @Lob
    private String responseBody;

    public enum DeliveryStatus {
        PENDING,
        SENT,
        FAILED,
        DEAD
    }
}
