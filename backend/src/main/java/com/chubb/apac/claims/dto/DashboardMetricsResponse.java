package com.chubb.apac.claims.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardMetricsResponse(
        long totalClaims,
        long openClaimsCount,
        long unassignedQueueCount,
        BigDecimal totalOutstandingLiabilityUsd,
        BigDecimal totalSettledPayoutUsd,
        Map<String, BigDecimal> liabilityByMarketUsd,
        Map<String, BigDecimal> liabilityByClaimTypeUsd,
        List<OfficerWorkloadSummary> officerWorkloads
) {
    public record OfficerWorkloadSummary(
            String officerName,
            long activeClaims,
            long settledClaims,
            long rejectedClaims,
            BigDecimal activeLiabilityExposureUsd
    ) {}
}
