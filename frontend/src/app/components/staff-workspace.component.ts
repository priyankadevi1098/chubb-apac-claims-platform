import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClaimService } from '../services/claim.service';
import { Claim, ClaimAuditEvent, DashboardMetrics } from '../models/claim.model';

@Component({
  selector: 'app-staff-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (metrics(); as m) {
      <div class="grid-4" style="margin-bottom:1rem;">
        <div class="card" style="margin:0; border-left:4px solid #2563eb;">
          <div style="font-size:0.8rem; color:#64748b;">OUTSTANDING LIABILITY EXPOSURE</div>
          <div style="font-size:1.6rem; font-weight:800; color:#0f172a;">
            \${{ m.totalOutstandingLiabilityUsd | number:'1.2-2' }}
          </div>
          <div style="font-size:0.8rem; color:#475569;">Across {{ m.openClaimsCount }} active open claims</div>
        </div>

        <div class="card" style="margin:0; border-left:4px solid #d97706;">
          <div style="font-size:0.8rem; color:#64748b;">UNASSIGNED TRIAGE QUEUE</div>
          <div style="font-size:1.6rem; font-weight:800; color:#b45309;">{{ m.unassignedQueueCount }}</div>
          <div style="font-size:0.8rem; color:#475569;">Awaiting officer pickup</div>
        </div>

        <div class="card" style="margin:0; border-left:4px solid #16a34a;">
          <div style="font-size:0.8rem; color:#64748b;">TOTAL SETTLED PAYOUTS</div>
          <div style="font-size:1.6rem; font-weight:800; color:#15803d;">
            \${{ m.totalSettledPayoutUsd | number:'1.2-2' }}
          </div>
          <div style="font-size:0.8rem; color:#475569;">Total historical claims: {{ m.totalClaims }}</div>
        </div>

        <div class="card" style="margin:0; border-left:4px solid #7c3aed;">
          <div style="font-size:0.8rem; color:#64748b;">EXPOSURE BY APAC MARKET</div>
          @for (entry of m.liabilityByMarketUsd | keyvalue; track entry.key) {
            <div style="font-size:0.82rem; display:flex; justify-content:space-between;">
              <strong>{{ entry.key }}:</strong>
              <span>\${{ entry.value | number:'1.0-0' }}</span>
            </div>
          }
        </div>
      </div>
    }

    <div class="grid-2">
      <div class="card">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.75rem;">
          <h3 style="margin:0;">Incoming & Active Claims Queue</h3>
          <div style="display:flex; gap:0.5rem; align-items:center;">
            <span style="font-size:0.85rem;">Acting Officer:</span>
            <input [(ngModel)]="activeOfficer" style="width:150px; margin:0;" />
          </div>
        </div>

        <table>
          <thead>
            <tr>
              <th>Ref / Market</th>
              <th>Type</th>
              <th>Status</th>
              <th>Reserve (USD)</th>
              <th>Officer</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            @for (c of claims(); track c.claimReference) {
              <tr [style.background]="selectedClaim()?.claimReference === c.claimReference ? '#eff6ff' : 'transparent'">
                <td>
                  <strong>{{ c.claimReference }}</strong>
                  <div style="font-size:0.75rem; color:#64748b;">{{ c.claimantName }} ({{ c.market }})</div>
                </td>
                <td>{{ c.claimType }}</td>
                <td><span class="badge badge-{{ c.status }}">{{ c.status }}</span></td>
                <td>\${{ c.reserveEstimateUsd | number:'1.2-2' }}</td>
                <td>{{ c.assignedOfficer || 'Unassigned' }}</td>
                <td>
                  <button (click)="selectClaim(c)" style="padding:0.35rem 0.65rem; font-size:0.8rem;">
                    Manage
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div class="card">
        @if (selectedClaim(); as sel) {
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <h3 style="margin:0;">Inspect & Adjudicate: {{ sel.claimReference }}</h3>
            <span class="badge badge-{{ sel.status }}">{{ sel.status }}</span>
          </div>

          <p style="font-size:0.88rem; color:#334155;">
            <strong>Policy:</strong> {{ sel.policyNumber }} |
            <strong>Claimed:</strong> \${{ sel.claimedAmountUsd | number:'1.2-2' }} |
            <strong>Current Liability Reserve:</strong> \${{ sel.reserveEstimateUsd | number:'1.2-2' }}
          </p>
          <p style="font-size:0.88rem; background:#f8fafc; padding:0.65rem; border-radius:6px;">
            {{ sel.incidentDescription }}
          </p>

          @if (actionError()) {
            <div style="padding:0.6rem; background:#fee2e2; color:#991b1b; border-radius:6px; margin-bottom:0.75rem;">
              {{ actionError() }}
            </div>
          }

          @if (sel.status !== 'SETTLED' && sel.status !== 'REJECTED') {
            <div style="border-top:1px solid #e2e8f0; padding-top:0.75rem; margin-top:0.75rem;">
              <h4>1. Pick Up / Update Reserve Liability</h4>
              <div style="display:flex; gap:0.5rem;">
                <input type="number" [(ngModel)]="updatedReserveInput" placeholder="Reserve Estimate USD" style="margin:0;" />
                <button (click)="assignToMe(sel.claimReference)">Assign to {{ activeOfficer }}</button>
              </div>

              <h4>2. Request Additional Information from Claimant</h4>
              <div style="display:flex; gap:0.5rem;">
                <input [(ngModel)]="infoPromptInput" placeholder="Specify documents/photos required..." style="margin:0;" />
                <button class="secondary" (click)="sendInfoRequest(sel.claimReference)">Request Info</button>
              </div>

              <h4>3. Final Settlement or Rejection Decision</h4>
              <div class="grid-2">
                <div>
                  <label>Approved Settlement Payout (USD)</label>
                  <input type="number" [(ngModel)]="settlementAmountInput" />
                </div>
                <div>
                  <label>Decision Rationale</label>
                  <input [(ngModel)]="decisionReasonInput" placeholder="Surveyor assessment verified..." />
                </div>
              </div>
              <div style="display:flex; gap:0.75rem;">
                <button class="success" (click)="decide(sel.claimReference, true)">Approve & Settle Claim</button>
                <button class="danger" (click)="decide(sel.claimReference, false)">Reject Claim</button>
              </div>
            </div>
          }

          <h4 style="margin-bottom:0.4rem;">Case Notes & Evidence Log</h4>
          @for (n of sel.notes; track n.createdAt) {
            <div style="font-size:0.8rem; padding:0.4rem; border-bottom:1px solid #f1f5f9;">
              <strong>[{{ n.authorRole }}] {{ n.author }}:</strong> {{ n.message }}
            </div>
          }
        } @else {
          <p style="color:#64748b;">Select a claim from the queue to review, request info, or settle.</p>
        }
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <h3 style="margin-top:0;">Team Workload & Performance</h3>
        @if (metrics(); as m) {
          <table>
            <thead>
              <tr>
                <th>Claims Officer</th>
                <th>Active Claims</th>
                <th>Settled</th>
                <th>Rejected</th>
                <th>Active Liability Managed</th>
              </tr>
            </thead>
            <tbody>
              @for (w of m.officerWorkloads; track w.officerName) {
                <tr>
                  <td><strong>{{ w.officerName }}</strong></td>
                  <td>{{ w.activeClaims }}</td>
                  <td>{{ w.settledClaims }}</td>
                  <td>{{ w.rejectedClaims }}</td>
                  <td>\${{ w.activeLiabilityExposureUsd | number:'1.2-2' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>

      <div class="card">
        <h3 style="margin-top:0;">Async Kafka Domain Event Stream (chubb.apac.claims.lifecycle.events)</h3>
        <div style="max-height:220px; overflow-y:auto;">
          @for (ev of auditEvents(); track ev.id) {
            <div style="font-size:0.8rem; padding:0.45rem; border-bottom:1px solid #e2e8f0;">
              <span class="badge" style="background:#e0e7ff; color:#1e40af; margin-right:0.4rem;">{{ ev.eventType }}</span>
              <strong>{{ ev.claimReference }}</strong> ({{ ev.previousStatus }} &rarr; {{ ev.newStatus }})
              by <em>{{ ev.actor }}</em>
              <span style="float:right; color:#64748b;">{{ ev.deliveryTransport }}</span>
              <div style="color:#475569; margin-top:0.15rem;">{{ ev.summary }}</div>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class StaffWorkspaceComponent implements OnInit {
  private readonly claimService = inject(ClaimService);

  claims = signal<Claim[]>([]);
  metrics = signal<DashboardMetrics | null>(null);
  auditEvents = signal<ClaimAuditEvent[]>([]);
  selectedClaim = signal<Claim | null>(null);
  actionError = signal<string>('');

  activeOfficer = 'Priya Nair';
  updatedReserveInput = 0;
  infoPromptInput = 'Please upload repair invoice and incident photos.';
  settlementAmountInput = 0;
  decisionReasonInput = 'Verified policy coverage and adjuster valuation report.';

  ngOnInit(): void {
    this.refreshAll();
  }

  refreshAll(): void {
    this.claimService.getAllClaims().subscribe((list) => {
      this.claims.set(list);
      if (!this.selectedClaim() && list.length > 0) {
        this.selectClaim(list[0]);
      } else if (this.selectedClaim()) {
        const updated = list.find((c) => c.claimReference === this.selectedClaim()?.claimReference);
        if (updated) this.selectClaim(updated);
      }
    });
    this.claimService.getDashboardMetrics().subscribe((m) => this.metrics.set(m));
    this.claimService.getRecentAuditEvents().subscribe((ev) => this.auditEvents.set(ev));
  }

  selectClaim(claim: Claim): void {
    this.actionError.set('');
    this.selectedClaim.set(claim);
    this.updatedReserveInput = claim.reserveEstimateUsd;
    this.settlementAmountInput = claim.claimedAmountUsd;
  }

  assignToMe(reference: string): void {
    this.actionError.set('');
    this.claimService.assignClaim(reference, this.activeOfficer, this.updatedReserveInput).subscribe({
      next: () => this.refreshAll(),
      error: (err) => this.actionError.set(err.error?.detail || 'Assignment failed.')
    });
  }

  sendInfoRequest(reference: string): void {
    this.actionError.set('');
    this.claimService.requestInfo(reference, this.activeOfficer, this.infoPromptInput).subscribe({
      next: () => this.refreshAll(),
      error: (err) => this.actionError.set(err.error?.detail || 'Request info failed.')
    });
  }

  decide(reference: string, approve: boolean): void {
    this.actionError.set('');
    this.claimService
      .recordDecision(reference, this.activeOfficer, approve, this.settlementAmountInput, this.decisionReasonInput)
      .subscribe({
        next: () => this.refreshAll(),
        error: (err) => this.actionError.set(err.error?.detail || 'Decision failed.')
      });
  }
}
