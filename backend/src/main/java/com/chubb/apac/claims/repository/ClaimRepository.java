package com.chubb.apac.claims.repository;

import com.chubb.apac.claims.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimReferenceIgnoreCase(String claimReference);
    List<Claim> findByClaimantEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<Claim> findAllByOrderByCreatedAtDesc();
}
