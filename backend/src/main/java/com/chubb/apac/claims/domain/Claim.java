package com.chubb.apac.claims.domain;

import com.chubb.apac.claims.domain.enums.ClaimStatus;
import com.chubb.apac.claims.domain.enums.ClaimType;
import com.chubb.apac.claims.domain.enums.MarketCode;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_reference", columnList = "claimReference", unique = true),
        @Index(name = "idx_claim_status", columnList = "status"),
        @Index(name = "idx_claim_officer", columnList = "assignedOfficer")
})
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String claimReference;

    @Column(nullable = false, length = 32)
    private String policyNumber;

    @Column(nullable = false, length = 100)
    private String claimantName;

    @Column(nullable = false, length = 120)
    private String claimantEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MarketCode market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ClaimStatus status;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 1000)
    private String incidentDescription;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal claimedAmountUsd;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal reserveEstimateUsd;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvedSettlementUsd;

    @Column(length = 80)
    private String assignedOfficer;

    @Column(length = 500)
    private String pendingInfoPrompt;

    @Column(length = 500)
    private String decisionReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "claim_notes", joinColumns = @JoinColumn(name = "claim_id"))
    private List<ClaimNote> notes = new ArrayList<>();

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addNote(String author, String role, String message) {
        this.notes.add(new ClaimNote(author, role, message, Instant.now()));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaimReference() { return claimReference; }
    public void setClaimReference(String claimReference) { this.claimReference = claimReference; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }
    public String getClaimantEmail() { return claimantEmail; }
    public void setClaimantEmail(String claimantEmail) { this.claimantEmail = claimantEmail; }
    public MarketCode getMarket() { return market; }
    public void setMarket(MarketCode market) { this.market = market; }
    public ClaimType getClaimType() { return claimType; }
    public void setClaimType(ClaimType claimType) { this.claimType = claimType; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public String getIncidentDescription() { return incidentDescription; }
    public void setIncidentDescription(String incidentDescription) { this.incidentDescription = incidentDescription; }
    public BigDecimal getClaimedAmountUsd() { return claimedAmountUsd; }
    public void setClaimedAmountUsd(BigDecimal claimedAmountUsd) { this.claimedAmountUsd = claimedAmountUsd; }
    public BigDecimal getReserveEstimateUsd() { return reserveEstimateUsd; }
    public void setReserveEstimateUsd(BigDecimal reserveEstimateUsd) { this.reserveEstimateUsd = reserveEstimateUsd; }
    public BigDecimal getApprovedSettlementUsd() { return approvedSettlementUsd; }
    public void setApprovedSettlementUsd(BigDecimal approvedSettlementUsd) { this.approvedSettlementUsd = approvedSettlementUsd; }
    public String getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(String assignedOfficer) { this.assignedOfficer = assignedOfficer; }
    public String getPendingInfoPrompt() { return pendingInfoPrompt; }
    public void setPendingInfoPrompt(String pendingInfoPrompt) { this.pendingInfoPrompt = pendingInfoPrompt; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public List<ClaimNote> getNotes() { return notes; }
    public void setNotes(List<ClaimNote> notes) { this.notes = notes; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
