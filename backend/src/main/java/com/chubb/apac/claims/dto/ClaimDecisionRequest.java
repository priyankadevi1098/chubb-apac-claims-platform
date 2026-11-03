package com.chubb.apac.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ClaimDecisionRequest(
        @NotBlank(message = "Officer name is required")
        String officerName,

        @NotNull(message = "Settlement decision flag is required")
        Boolean approveSettlement,

        BigDecimal approvedSettlementUsd,

        @NotBlank(message = "Decision reason is required")
        String reason
) {}
