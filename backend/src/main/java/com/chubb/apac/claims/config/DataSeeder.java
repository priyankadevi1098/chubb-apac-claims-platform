package com.chubb.apac.claims.config;

import com.chubb.apac.claims.domain.Claim;
import com.chubb.apac.claims.domain.enums.ClaimStatus;
import com.chubb.apac.claims.domain.enums.ClaimType;
import com.chubb.apac.claims.domain.enums.MarketCode;
import com.chubb.apac.claims.messaging.ClaimEventPayload;
import com.chubb.apac.claims.messaging.ClaimEventPublisher;
import com.chubb.apac.claims.repository.ClaimRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ClaimRepository claimRepository;
    private final ClaimEventPublisher eventPublisher;

    public DataSeeder(ClaimRepository claimRepository, ClaimEventPublisher eventPublisher) {
        this.claimRepository = claimRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(String... args) {
        if (claimRepository.count() > 0) return;

        Claim c1 = new Claim();
        c1.setClaimReference("CHB-SG-2026-1001");
        c1.setPolicyNumber("POL-SG-88412");
        c1.setClaimantName("Wei Ming Tan");
        c1.setClaimantEmail("weiming.tan@example.sg");
        c1.setMarket(MarketCode.SG);
        c1.setClaimType(ClaimType.MOTOR);
        c1.setStatus(ClaimStatus.SUBMITTED);
        c1.setIncidentDate(LocalDate.now().minusDays(2));
        c1.setIncidentDescription("Rear-end collision on PIE expressway during heavy rain; rear bumper and sensors damaged.");
        c1.setClaimedAmountUsd(new BigDecimal("4200.00"));
        c1.setReserveEstimateUsd(new BigDecimal("4200.00"));
        c1.addNote("Wei Ming Tan", "CLAIMANT", "Submitted dashcam stills and workshop quotation.");
        claimRepository.save(c1);

        Claim c2 = new Claim();
        c2.setClaimReference("CHB-AU-2026-1002");
        c2.setPolicyNumber("POL-AU-33910");
        c2.setClaimantName("Sarah Jenkins");
        c2.setClaimantEmail("s.jenkins@example.com.au");
        c2.setMarket(MarketCode.AU);
        c2.setClaimType(ClaimType.PROPERTY);
        c2.setStatus(ClaimStatus.INFO_REQUESTED);
        c2.setAssignedOfficer("Priya Nair");
        c2.setIncidentDate(LocalDate.now().minusDays(5));
        c2.setIncidentDescription("Hailstorm roof tile damage causing water ingress into master bedroom ceiling.");
        c2.setClaimedAmountUsd(new BigDecimal("18500.00"));
        c2.setReserveEstimateUsd(new BigDecimal("21275.00"));
        c2.setPendingInfoPrompt("Please provide licensed roofer structural inspection report and invoice breakdown.");
        c2.addNote("Sarah Jenkins", "CLAIMANT", "Initial property storm claim lodged.");
        c2.addNote("Priya Nair", "STAFF", "Requested Info: Please provide licensed roofer structural inspection report.");
        claimRepository.save(c2);

        Claim c3 = new Claim();
        c3.setClaimReference("CHB-HK-2026-1003");
        c3.setPolicyNumber("POL-HK-77201");
        c3.setClaimantName("Raymond Cheung");
        c3.setClaimantEmail("r.cheung@example.hk");
        c3.setMarket(MarketCode.HK);
        c3.setClaimType(ClaimType.PROPERTY);
        c3.setStatus(ClaimStatus.UNDER_REVIEW);
        c3.setAssignedOfficer("Marcus Lim");
        c3.setIncidentDate(LocalDate.now().minusDays(4));
        c3.setIncidentDescription("Burst pipe in kitchen flooding hardwood flooring across living room.");
        c3.setClaimedAmountUsd(new BigDecimal("12000.00"));
        c3.setReserveEstimateUsd(new BigDecimal("13800.00"));
        c3.addNote("Marcus Lim", "STAFF", "Assigned for loss adjuster valuation review.");
        claimRepository.save(c3);

        Claim c4 = new Claim();
        c4.setClaimReference("CHB-JP-2026-1004");
        c4.setPolicyNumber("POL-JP-55192");
        c4.setClaimantName("Kenji Sato");
        c4.setClaimantEmail("kenji.sato@example.jp");
        c4.setMarket(MarketCode.JP);
        c4.setClaimType(ClaimType.MOTOR);
        c4.setStatus(ClaimStatus.SETTLED);
        c4.setAssignedOfficer("Priya Nair");
        c4.setIncidentDate(LocalDate.now().minusDays(9));
        c4.setIncidentDescription("Side mirror and passenger door scrape in automated parking garage.");
        c4.setClaimedAmountUsd(new BigDecimal("2800.00"));
        c4.setReserveEstimateUsd(BigDecimal.ZERO);
        c4.setApprovedSettlementUsd(new BigDecimal("2650.00"));
        c4.setDecisionReason("Approved workshop repair less policy excess ($150).");
        c4.addNote("Priya Nair", "STAFF", "Claim SETTLED for $2650.00.");
        claimRepository.save(c4);

        eventPublisher.publish(new ClaimEventPayload(
                c1.getClaimReference(), "CLAIM_SUBMITTED", "NONE", "SUBMITTED",
                c1.getClaimantName(), c1.getReserveEstimateUsd(),
                "Seeded SG Motor Claim in queue", Instant.now()
        ));
        eventPublisher.publish(new ClaimEventPayload(
                c2.getClaimReference(), "ADDITIONAL_INFO_REQUESTED", "UNDER_REVIEW", "INFO_REQUESTED",
                "Priya Nair", c2.getReserveEstimateUsd(),
                "Awaiting roofer inspection report from Sarah Jenkins", Instant.now()
        ));
    }
}
