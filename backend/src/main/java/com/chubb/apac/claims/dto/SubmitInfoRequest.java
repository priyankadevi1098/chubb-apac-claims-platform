package com.chubb.apac.claims.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitInfoRequest(
        @NotBlank(message = "Claimant response is required")
        String responseText
) {}
