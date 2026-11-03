package com.chubb.apac.claims.service;

import com.chubb.apac.claims.domain.Claim;
import com.chubb.apac.claims.domain.enums.ClaimStatus;
import com.chubb.apac.claims.domain.enums.ClaimType;
import com.chubb.apac.claims.domain.enums.MarketCode;
import com.chubb.apac.claims.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ClaimServiceTest {

    @Autowired
    private ClaimService claimService;

    @Test
    void shouldLifecycleClaimFromSubmissionToSettlementAndReleaseLiability() {
        CreateClaimRequest createReq = new CreateClaimRequest(
                "POL-MY-99887",
                "Aisha Rahman",
                "aisha@example.my",
                MarketCode.MY,
                ClaimType.PROPERTY,
                LocalDate.now().minusDays(1),
                "Lightning strike damaged home inverter and main distribution board.",
                new BigDecimal("10000.00")
        );

        Claim submitted = claimService.submitClaim(createReq);
        assertEquals(ClaimStatus.SUBMITTED, submitted.getStatus());
        assertEquals(new BigDecimal("11500.00"), submitted.getReserveEstimateUsd());

        Claim assigned = claimService.pickupAndAssignClaim(
                submitted.getClaimReference(),
                new AssignClaimRequest("Daniel Wong", new BigDecimal("11000.00"))
        );
        assertEquals(ClaimStatus.UNDER_REVIEW, assigned.getStatus());
        assertEquals("Daniel Wong", assigned.getAssignedOfficer());
        assertEquals(new BigDecimal("11000.00"), assigned.getReserveEstimateUsd());

        Claim infoReq = claimService.requestAdditionalInformation(
                submitted.getClaimReference(),
                new InfoRequestDto("Daniel Wong", "Upload TNB utility incident report.")
        );
        assertEquals(ClaimStatus.INFO_REQUESTED, infoReq.getStatus());
        assertNotNull(infoReq.getPendingInfoPrompt());

        Claim infoProvided = claimService.submitAdditionalInformation(
                submitted.getClaimReference(),
                new SubmitInfoRequest("Uploaded TNB report ref #TNB-2026-441.")
        );
        assertEquals(ClaimStatus.UNDER_REVIEW, infoProvided.getStatus());
        assertNull(infoProvided.getPendingInfoPrompt());

        Claim settled = claimService.recordDecision(
                submitted.getClaimReference(),
                new ClaimDecisionRequest("Daniel Wong", true, new BigDecimal("9800.00"), "Approved full replacement minus deductible")
        );
        assertEquals(ClaimStatus.SETTLED, settled.getStatus());
        assertEquals(BigDecimal.ZERO, settled.getReserveEstimateUsd());
        assertEquals(new BigDecimal("9800.00"), settled.getApprovedSettlementUsd());

        assertThrows(IllegalStateException.class, () ->
                claimService.recordDecision(
                        submitted.getClaimReference(),
                        new ClaimDecisionRequest("Daniel Wong", false, BigDecimal.ZERO, "Invalid double decision")
                )
        );
    }
}
