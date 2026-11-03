package com.chubb.apac.claims.dto;

import jakarta.validation.constraints.NotBlank;

public record InfoRequestDto(
        @NotBlank(message = "Officer name is required")
        String officerName,

        @NotBlank(message = "Information request prompt is required")
        String prompt
) {}
