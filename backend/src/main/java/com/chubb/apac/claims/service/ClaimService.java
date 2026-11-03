package com.chubb.apac.claims.service;

import com.chubb.apac.claims.domain.Claim;
import com.chubb.apac.claims.domain.enums.ClaimStatus;
import com.chubb.apac.claims.domain.enums.ClaimType;
import com.chubb.apac.claims.dto.*;
import com.chubb.apac.claims.messaging.ClaimEventPayload;
import com.chubb.apac.claims.messaging.ClaimEventPublisher;
import com.chubb.apac.claims.repository.ClaimRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimEventPublisher eventPublisher;
    private final AtomicInteger sequence = new AtomicInteger(1004);

    public ClaimService(ClaimRepository claimRepository, ClaimEventPublisher eventPublisher) {
        this.claimRepository = claimRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Claim submitClaim(CreateClaimRequest request) {
        Claim claim = new Claim();
        String ref = String.format("CHB-%s-%d-%04d",
                request.market().name(), Year.now().getValue(), sequence.incrementAndGet());

        claim.setClaimReference(ref);
        claim.setPolicyNumber(request.policyNumber().trim().toUpperCase());
        claim.setClaimantName(request.claimantName().trim());
        claim.setClaimantEmail(request.claimantEmail().trim().toLowerCase());
        claim.setMarket(request.market());
        claim.setClaimType(request.claimType());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setIncidentDate(request.incidentDate());
        claim.setIncidentDescription(request.incidentDescription().trim());
        claim.setClaimedAmountUsd(request.claimedAmountUsd());

        BigDecimal multiplier = request.claimType() == ClaimType.PROPERTY
                ? new BigDecimal("1.15") : BigDecimal.ONE;
        claim.setReserveEstimateUsd(request.claimedAmountUsd().multiply(multiplier).setScale(2, RoundingMode.HALF_UP));

        claim.addNote(request.claimantName(), "CLAIMANT", "Initial FNOL (First Notice of Loss) submitted.");
        Claim saved = claimRepository.save(claim);

        eventPublisher.publish(new ClaimEventPayload(
                saved.getClaimReference(),
                "CLAIM_SUBMITTED",
                "NONE",
                saved.getStatus().name(),
                saved.getClaimantName(),
                saved.getReserveEstimateUsd(),
                "New " + saved.getClaimType() + " claim submitted in " + saved.getMarket(),
                Instant.now()
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Claim> getAllClaims() {
        return claimRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Claim getByReference(String reference) {
        return claimRepository.findByClaimReferenceIgnoreCase(reference.trim())
                .orElseThrow(() -> new EntityNotFoundException("Claim not found: " + reference));
    }

    @Transactional
    public Claim pickupAndAssignClaim(String reference, AssignClaimRequest request) {
        Claim claim = getByReference(reference);
        if (claim.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot assign a claim that is already " + claim.getStatus());
        }

        String prevStatus = claim.getStatus().name();
        claim.setAssignedOfficer(request.officerName().trim());
        claim.setStatus(ClaimStatus.UNDER_REVIEW);

        if (request.updatedReserveEstimateUsd() != null &&
                request.updatedReserveEstimateUsd().compareTo(BigDecimal.ZERO) > 0) {
            claim.setReserveEstimateUsd(request.updatedReserveEstimateUsd());
        }

        claim.addNote(request.officerName(), "STAFF",
                "Claim picked up for review. Reserve liability set at $" + claim.getReserveEstimateUsd());
        Claim saved = claimRepository.save(claim);

        eventPublisher.publish(new ClaimEventPayload(
                saved.getClaimReference(),
                "CLAIM_ASSIGNED",
                prevStatus,
                saved.getStatus().name(),
                request.officerName(),
                saved.getReserveEstimateUsd(),
                "Assigned to officer " + request.officerName(),
                Instant.now()
        ));

        return saved;
    }

    @Transactional
    public Claim requestAdditionalInformation(String reference, InfoRequestDto request) {
        Claim claim = getByReference(reference);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW && claim.getStatus() != ClaimStatus.SUBMITTED) {
            throw new IllegalStateException("Additional info can only be requested from SUBMITTED or UNDER_REVIEW state.");
        }

        String prevStatus = claim.getStatus().name();
        claim.setAssignedOfficer(request.officerName().trim());
        claim.setStatus(ClaimStatus.INFO_REQUESTED);
        claim.setPendingInfoPrompt(request.prompt().trim());
        claim.addNote(request.officerName(), "STAFF", "Requested Info: " + request.prompt().trim());

        Claim saved = claimRepository.save(claim);

        eventPublisher.publish(new ClaimEventPayload(
                saved.getClaimReference(),
                "ADDITIONAL_INFO_REQUESTED",
                prevStatus,
                saved.getStatus().name(),
                request.officerName(),
                saved.getReserveEstimateUsd(),
                "Requested additional details from claimant: " + request.prompt(),
                Instant.now()
        ));

        return saved;
    }

    @Transactional
    public Claim submitAdditionalInformation(String reference, SubmitInfoRequest request) {
        Claim claim = getByReference(reference);
        if (claim.getStatus() != ClaimStatus.INFO_REQUESTED) {
            throw new IllegalStateException("Claim is not currently awaiting additional information.");
        }

        String prevStatus = claim.getStatus().name();
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setPendingInfoPrompt(null);
        claim.addNote(claim.getClaimantName(), "CLAIMANT",
                "Provided requested info: " + request.responseText().trim());

        Claim saved = claimRepository.save(claim);

        eventPublisher.publish(new ClaimEventPayload(
                saved.getClaimReference(),
                "ADDITIONAL_INFO_PROVIDED",
                prevStatus,
                saved.getStatus().name(),
                claim.getClaimantName(),
                saved.getReserveEstimateUsd(),
                "Claimant responded to information request; returned to UNDER_REVIEW",
                Instant.now()
        ));

        return saved;
    }

    @Transactional
    public Claim recordDecision(String reference, ClaimDecisionRequest request) {
        Claim claim = getByReference(reference);
        if (claim.getStatus().isTerminal()) {
            throw new IllegalStateException("Claim has already reached a final decision: " + claim.getStatus());
        }

        String prevStatus = claim.getStatus().name();
        claim.setAssignedOfficer(request.officerName().trim());
        claim.setDecisionReason(request.reason().trim());
        claim.setPendingInfoPrompt(null);

        if (Boolean.TRUE.equals(request.approveSettlement())) {
            BigDecimal settlement = request.approvedSettlementUsd() != null
                    ? request.approvedSettlementUsd()
                    : claim.getClaimedAmountUsd();
            claim.setStatus(ClaimStatus.SETTLED);
            claim.setApprovedSettlementUsd(settlement);
            claim.setReserveEstimateUsd(BigDecimal.ZERO);
            claim.addNote(request.officerName(), "STAFF",
                    "Claim SETTLED for $" + settlement + ". Reason: " + request.reason());
        } else {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setApprovedSettlementUsd(BigDecimal.ZERO);
            claim.setReserveEstimateUsd(BigDecimal.ZERO);
            claim.addNote(request.officerName(), "STAFF",
                    "Claim REJECTED. Reason: " + request.reason());
        }

        Claim saved = claimRepository.save(claim);

        eventPublisher.publish(new ClaimEventPayload(
                saved.getClaimReference(),
                saved.getStatus() == ClaimStatus.SETTLED ? "CLAIM_SETTLED" : "CLAIM_REJECTED",
                prevStatus,
                saved.getStatus().name(),
                request.officerName(),
                BigDecimal.ZERO,
                "Decision recorded (" + saved.getStatus() + "): " + request.reason(),
                Instant.now()
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics() {
        List<Claim> allClaims = claimRepository.findAll();

        long totalClaims = allClaims.size();
        long openClaimsCount = allClaims.stream()
                .filter(c -> c.getStatus().isOutstandingLiability())
                .count();
        long unassignedQueueCount = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.SUBMITTED &&
                        (c.getAssignedOfficer() == null || c.getAssignedOfficer().isBlank()))
                .count();

        BigDecimal totalOutstandingLiabilityUsd = allClaims.stream()
                .filter(c -> c.getStatus().isOutstandingLiability())
                .map(Claim::getReserveEstimateUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSettledPayoutUsd = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.SETTLED && c.getApprovedSettlementUsd() != null)
                .map(Claim::getApprovedSettlementUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> liabilityByMarket = allClaims.stream()
                .filter(c -> c.getStatus().isOutstandingLiability())
                .collect(Collectors.groupingBy(
                        c -> c.getMarket().name(),
                        Collectors.reducing(BigDecimal.ZERO, Claim::getReserveEstimateUsd, BigDecimal::add)
                ));

        Map<String, BigDecimal> liabilityByType = allClaims.stream()
                .filter(c -> c.getStatus().isOutstandingLiability())
                .collect(Collectors.groupingBy(
                        c -> c.getClaimType().name(),
                        Collectors.reducing(BigDecimal.ZERO, Claim::getReserveEstimateUsd, BigDecimal::add)
                ));

        Map<String, List<Claim>> byOfficer = allClaims.stream()
                .filter(c -> c.getAssignedOfficer() != null && !c.getAssignedOfficer().isBlank())
                .collect(Collectors.groupingBy(Claim::getAssignedOfficer));

        List<DashboardMetricsResponse.OfficerWorkloadSummary> workloads = byOfficer.entrySet().stream()
                .map(entry -> {
                    String officer = entry.getKey();
                    List<Claim> assigned = entry.getValue();
                    long active = assigned.stream().filter(c -> c.getStatus().isOutstandingLiability()).count();
                    long settled = assigned.stream().filter(c -> c.getStatus() == ClaimStatus.SETTLED).count();
                    long rejected = assigned.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
                    BigDecimal exposure = assigned.stream()
                            .filter(c -> c.getStatus().isOutstandingLiability())
                            .map(Claim::getReserveEstimateUsd)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DashboardMetricsResponse.OfficerWorkloadSummary(
                            officer, active, settled, rejected, exposure
                    );
                })
                .sorted(Comparator.comparingLong(DashboardMetricsResponse.OfficerWorkloadSummary::activeClaims).reversed())
                .toList();

        return new DashboardMetricsResponse(
                totalClaims,
                openClaimsCount,
                unassignedQueueCount,
                totalOutstandingLiabilityUsd,
                totalSettledPayoutUsd,
                liabilityByMarket,
                liabilityByType,
                workloads
        );
    }
}
