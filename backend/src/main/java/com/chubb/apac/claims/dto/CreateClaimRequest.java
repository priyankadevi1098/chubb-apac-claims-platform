package com.chubb.apac.claims.dto;

import com.chubb.apac.claims.domain.enums.ClaimType;
import com.chubb.apac.claims.domain.enums.MarketCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClaimRequest(
        @NotBlank(message = "Policy number is required")
        String policyNumber,

        @NotBlank(message = "Claimant name is required")
        String claimantName,

        @NotBlank @Email(message = "Valid claimant email is required")
        String claimantEmail,

        @NotNull(message = "Market code is required")
        MarketCode market,

        @NotNull(message = "Claim type is required")
        ClaimType claimType,

        @NotNull @PastOrPresent(message = "Incident date cannot be in the future")
        LocalDate incidentDate,

        @NotBlank @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        String incidentDescription,

        @NotNull @DecimalMin(value = "10.00", message = "Claimed amount must be at least $10")
        BigDecimal claimedAmountUsd
) {}
