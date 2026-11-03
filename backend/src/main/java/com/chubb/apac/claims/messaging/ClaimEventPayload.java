package com.chubb.apac.claims.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimEventPayload(
        String claimReference,
        String eventType,
        String previousStatus,
        String newStatus,
        String actor,
        BigDecimal reserveExposureUsd,
        String summary,
        Instant occurredAt
) {}
