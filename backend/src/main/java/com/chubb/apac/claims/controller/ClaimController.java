package com.chubb.apac.claims.controller;

import com.chubb.apac.claims.domain.Claim;
import com.chubb.apac.claims.domain.ClaimAuditEvent;
import com.chubb.apac.claims.dto.*;
import com.chubb.apac.claims.repository.ClaimAuditEventRepository;
import com.chubb.apac.claims.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimAuditEventRepository auditRepository;

    public ClaimController(ClaimService claimService, ClaimAuditEventRepository auditRepository) {
        this.claimService = claimService;
        this.auditRepository = auditRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Claim submitClaim(@Valid @RequestBody CreateClaimRequest request) {
        return claimService.submitClaim(request);
    }

    @GetMapping
    public List<Claim> listAllClaims() {
        return claimService.getAllClaims();
    }

    @GetMapping("/{reference}")
    public Claim getClaimByReference(@PathVariable String reference) {
        return claimService.getByReference(reference);
    }

    @PatchMapping("/{reference}/assign")
    public Claim assignClaim(
            @PathVariable String reference,
            @Valid @RequestBody AssignClaimRequest request) {
        return claimService.pickupAndAssignClaim(reference, request);
    }

    @PostMapping("/{reference}/request-info")
    public Claim requestAdditionalInfo(
            @PathVariable String reference,
            @Valid @RequestBody InfoRequestDto request) {
        return claimService.requestAdditionalInformation(reference, request);
    }

    @PostMapping("/{reference}/submit-info")
    public Claim submitAdditionalInfo(
            @PathVariable String reference,
            @Valid @RequestBody SubmitInfoRequest request) {
        return claimService.submitAdditionalInformation(reference, request);
    }

    @PostMapping("/{reference}/decision")
    public Claim decideClaim(
            @PathVariable String reference,
            @Valid @RequestBody ClaimDecisionRequest request) {
        return claimService.recordDecision(reference, request);
    }

    @GetMapping("/dashboard/metrics")
    public DashboardMetricsResponse getDashboardMetrics() {
        return claimService.getDashboardMetrics();
    }

    @GetMapping("/events/recent")
    public List<ClaimAuditEvent> getRecentAuditEvents() {
        return auditRepository.findTop30ByOrderByOccurredAtDesc();
    }
}
