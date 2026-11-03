package com.chubb.apac.claims.repository;

import com.chubb.apac.claims.domain.ClaimAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimAuditEventRepository extends JpaRepository<ClaimAuditEvent, Long> {
    List<ClaimAuditEvent> findTop30ByOrderByOccurredAtDesc();
    List<ClaimAuditEvent> findByClaimReferenceOrderByOccurredAtDesc(String claimReference);
}
