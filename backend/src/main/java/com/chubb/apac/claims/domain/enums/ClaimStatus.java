package com.chubb.apac.claims.domain.enums;

public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    INFO_REQUESTED,
    SETTLED,
    REJECTED;

    public boolean isTerminal() {
        return this == SETTLED || this == REJECTED;
    }

    public boolean isOutstandingLiability() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == INFO_REQUESTED;
    }
}
