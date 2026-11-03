export type MarketCode = 'SG' | 'HK' | 'AU' | 'MY' | 'TH' | 'JP';
export type ClaimType = 'MOTOR' | 'PROPERTY';
export type ClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'INFO_REQUESTED' | 'SETTLED' | 'REJECTED';

export interface ClaimNote {
  author: string;
  authorRole: 'CLAIMANT' | 'STAFF';
  message: string;
  createdAt: string;
}

export interface Claim {
  id: number;
  claimReference: string;
  policyNumber: string;
  claimantName: string;
  claimantEmail: string;
  market: MarketCode;
  claimType: ClaimType;
  status: ClaimStatus;
  incidentDate: string;
  incidentDescription: string;
  claimedAmountUsd: number;
  reserveEstimateUsd: number;
  approvedSettlementUsd?: number;
  assignedOfficer?: string;
  pendingInfoPrompt?: string;
  decisionReason?: string;
  notes: ClaimNote[];
  createdAt: string;
  updatedAt: string;
}

export interface OfficerWorkloadSummary {
  officerName: string;
  activeClaims: number;
  settledClaims: number;
  rejectedClaims: number;
  activeLiabilityExposureUsd: number;
}

export interface DashboardMetrics {
  totalClaims: number;
  openClaimsCount: number;
  unassignedQueueCount: number;
  totalOutstandingLiabilityUsd: number;
  totalSettledPayoutUsd: number;
  liabilityByMarketUsd: Record<string, number>;
  liabilityByClaimTypeUsd: Record<string, number>;
  officerWorkloads: OfficerWorkloadSummary[];
}

export interface ClaimAuditEvent {
  id: number;
  claimReference: string;
  eventType: string;
  previousStatus: string;
  newStatus: string;
  actor: string;
  reserveExposureUsd: number;
  summary: string;
  deliveryTransport: string;
  occurredAt: string;
}
