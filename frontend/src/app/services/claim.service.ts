import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Claim, ClaimAuditEvent, DashboardMetrics } from '../models/claim.model';

@Injectable({ providedIn: 'root' })
export class ClaimService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1/claims';

  getAllClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(this.baseUrl);
  }

  getClaimByReference(reference: string): Observable<Claim> {
    return this.http.get<Claim>(`${this.baseUrl}/${encodeURIComponent(reference.trim())}`);
  }

  submitClaim(payload: any): Observable<Claim> {
    return this.http.post<Claim>(this.baseUrl, payload);
  }

  assignClaim(reference: string, officerName: string, updatedReserveEstimateUsd?: number): Observable<Claim> {
    return this.http.patch<Claim>(`${this.baseUrl}/${reference}/assign`, {
      officerName,
      updatedReserveEstimateUsd
    });
  }

  requestInfo(reference: string, officerName: string, prompt: string): Observable<Claim> {
    return this.http.post<Claim>(`${this.baseUrl}/${reference}/request-info`, {
      officerName,
      prompt
    });
  }

  submitAdditionalInfo(reference: string, responseText: string): Observable<Claim> {
    return this.http.post<Claim>(`${this.baseUrl}/${reference}/submit-info`, {
      responseText
    });
  }

  recordDecision(
    reference: string,
    officerName: string,
    approveSettlement: boolean,
    approvedSettlementUsd: number | null,
    reason: string
  ): Observable<Claim> {
    return this.http.post<Claim>(`${this.baseUrl}/${reference}/decision`, {
      officerName,
      approveSettlement,
      approvedSettlementUsd,
      reason
    });
  }

  getDashboardMetrics(): Observable<DashboardMetrics> {
    return this.http.get<DashboardMetrics>(`${this.baseUrl}/dashboard/metrics`);
  }

  getRecentAuditEvents(): Observable<ClaimAuditEvent[]> {
    return this.http.get<ClaimAuditEvent[]>(`${this.baseUrl}/events/recent`);
  }
}
