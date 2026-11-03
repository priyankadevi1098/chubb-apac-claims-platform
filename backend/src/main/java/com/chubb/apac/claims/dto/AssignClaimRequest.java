package com.chubb.apac.claims.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AssignClaimRequest(
        @NotBlank(message = "Officer name is required")
        String officerName,
        BigDecimal updatedReserveEstimateUsd
) {}
