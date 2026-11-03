package com.chubb.apac.claims.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "claim_audit_events")
public class ClaimAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String claimReference;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String previousStatus;

    @Column(nullable = false)
    private String newStatus;

    @Column(nullable = false)
    private String actor;

    @Column(precision = 15, scale = 2)
    private BigDecimal reserveExposureUsd;

    @Column(length = 600)
    private String summary;

    @Column(nullable = false)
    private String deliveryTransport;

    @Column(nullable = false)
    private Instant occurredAt;

    public ClaimAuditEvent() {}

    public ClaimAuditEvent(String claimReference, String eventType, String previousStatus,
                           String newStatus, String actor, BigDecimal reserveExposureUsd,
                           String summary, String deliveryTransport, Instant occurredAt) {
        this.claimReference = claimReference;
        this.eventType = eventType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actor = actor;
        this.reserveExposureUsd = reserveExposureUsd;
        this.summary = summary;
        this.deliveryTransport = deliveryTransport;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getClaimReference() { return claimReference; }
    public String getEventType() { return eventType; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public String getActor() { return actor; }
    public BigDecimal getReserveExposureUsd() { return reserveExposureUsd; }
    public String getSummary() { return summary; }
    public String getDeliveryTransport() { return deliveryTransport; }
    public Instant getOccurredAt() { return occurredAt; }
}
