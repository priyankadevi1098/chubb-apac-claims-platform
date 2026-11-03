import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClaimService } from '../services/claim.service';
import { Claim } from '../models/claim.model';

@Component({
  selector: 'app-claimant-portal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="grid-2">
      <div class="card">
        <h2 style="margin-top:0">Track Your Claim</h2>
        <p style="color:#475569; font-size:0.9rem;">
          Enter your Chubb Claim Reference (e.g., <strong>CHB-AU-2026-1002</strong> or <strong>CHB-SG-2026-1001</strong>)
        </p>
        <div style="display:flex; gap:0.5rem; margin-bottom:1rem;">
          <input [(ngModel)]="searchReference" placeholder="CHB-AU-2026-1002" style="margin:0;" />
          <button (click)="lookupClaim()">Track</button>
        </div>

        @if (errorMsg()) {
          <div style="padding:0.75rem; background:#fee2e2; color:#991b1b; border-radius:6px; margin-bottom:1rem;">
            {{ errorMsg() }}
          </div>
        }

        @if (trackedClaim(); as claim) {
          <div style="border-top:1px solid #e2e8f0; padding-top:1rem;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <h3 style="margin:0;">{{ claim.claimReference }} ({{ claim.market }} - {{ claim.claimType }})</h3>
              <span class="badge badge-{{ claim.status }}">{{ claim.status }}</span>
            </div>

            <p style="font-size:0.9rem; color:#334155;">
              <strong>Policy:</strong> {{ claim.policyNumber }} |
              <strong>Claimed Amount:</strong> \${{ claim.claimedAmountUsd | number:'1.2-2' }} |
              <strong>Assigned Officer:</strong> {{ claim.assignedOfficer || 'Awaiting Assignment' }}
            </p>
            <p style="font-size:0.9rem;"><strong>Incident ({{ claim.incidentDate }}):</strong> {{ claim.incidentDescription }}</p>

            @if (claim.status === 'INFO_REQUESTED') {
              <div style="background:#fff7ed; border:1px solid #fdba74; padding:1rem; border-radius:6px; margin:1rem 0;">
                <strong style="color:#c2410c;">Action Required — Claims Officer Requested Information:</strong>
                <p style="margin:0.5rem 0;">"{{ claim.pendingInfoPrompt }}"</p>
                <textarea [(ngModel)]="infoResponseText" rows="3" placeholder="Provide the requested details or document references..."></textarea>
                <button (click)="submitRequestedInfo(claim.claimReference)">Submit Additional Information</button>
              </div>
            }

            @if (claim.status === 'SETTLED' || claim.status === 'REJECTED') {
              <div style="background:#f1f5f9; padding:1rem; border-radius:6px; margin:1rem 0;">
                <strong>Final Decision ({{ claim.status }}):</strong>
                @if (claim.status === 'SETTLED') {
                  <p style="margin:0.35rem 0; color:#166534; font-weight:700;">
                    Approved Settlement Payout: \${{ claim.approvedSettlementUsd | number:'1.2-2' }} USD
                  </p>
                }
                <p style="margin:0.25rem 0;">Reason: {{ claim.decisionReason }}</p>
              </div>
            }

            <h4>Claim Activity & Communication Timeline</h4>
            @for (note of claim.notes; track note.createdAt) {
              <div style="font-size:0.85rem; padding:0.5rem; border-left:3px solid #2563eb; background:#f8fafc; margin-bottom:0.5rem;">
                <strong>{{ note.author }} ({{ note.authorRole }})</strong>
                <span style="color:#64748b;"> — {{ note.createdAt | date:'short' }}</span>
                <div>{{ note.message }}</div>
              </div>
            }
          </div>
        }
      </div>

      <div class="card">
        <h2 style="margin-top:0">Report a New Incident (FNOL)</h2>
        @if (submissionSuccessRef()) {
          <div style="padding:0.85rem; background:#dcfce7; color:#166534; border-radius:6px; margin-bottom:1rem;">
            Claim submitted! Your tracking reference is <strong>{{ submissionSuccessRef() }}</strong>.
          </div>
        }

        <label>Policy Number</label>
        <input [(ngModel)]="newClaim.policyNumber" placeholder="e.g., POL-SG-90122" />

        <div class="grid-2">
          <div>
            <label>Your Full Name</label>
            <input [(ngModel)]="newClaim.claimantName" placeholder="Full Name" />
          </div>
          <div>
            <label>Email Address</label>
            <input [(ngModel)]="newClaim.claimantEmail" type="email" placeholder="name@example.com" />
          </div>
        </div>

        <div class="grid-2">
          <div>
            <label>APAC Market</label>
            <select [(ngModel)]="newClaim.market">
              <option value="SG">Singapore (SG)</option>
              <option value="HK">Hong Kong (HK)</option>
              <option value="AU">Australia (AU)</option>
              <option value="MY">Malaysia (MY)</option>
              <option value="TH">Thailand (TH)</option>
              <option value="JP">Japan (JP)</option>
            </select>
          </div>
          <div>
            <label>Line of Business</label>
            <select [(ngModel)]="newClaim.claimType">
              <option value="MOTOR">Motor Insurance</option>
              <option value="PROPERTY">Property Insurance</option>
            </select>
          </div>
        </div>

        <div class="grid-2">
          <div>
            <label>Incident Date</label>
            <input type="date" [(ngModel)]="newClaim.incidentDate" />
          </div>
          <div>
            <label>Estimated Claim Amount (USD)</label>
            <input type="number" [(ngModel)]="newClaim.claimedAmountUsd" />
          </div>
        </div>

        <label>Incident Description</label>
        <textarea rows="3" [(ngModel)]="newClaim.incidentDescription"
                  placeholder="Describe the incident, damages, and any third-party involvement (min 10 chars)..."></textarea>

        <button (click)="submitNewClaim()">Submit Claim</button>
      </div>
    </div>
  `
})
export class ClaimantPortalComponent implements OnInit {
  private readonly claimService = inject(ClaimService);

  searchReference = 'CHB-AU-2026-1002';
  trackedClaim = signal<Claim | null>(null);
  errorMsg = signal<string>('');
  infoResponseText = '';
  submissionSuccessRef = signal<string>('');

  newClaim = {
    policyNumber: 'POL-SG-50291',
    claimantName: 'Linh Nguyen',
    claimantEmail: 'linh@example.sg',
    market: 'SG',
    claimType: 'MOTOR',
    incidentDate: new Date().toISOString().split('T')[0],
    claimedAmountUsd: 3500,
    incidentDescription: 'Front headlight and fender damaged in multi-lane merge on AYE.'
  };

  ngOnInit(): void {
    this.lookupClaim();
  }

  lookupClaim(): void {
    if (!this.searchReference.trim()) return;
    this.errorMsg.set('');
    this.claimService.getClaimByReference(this.searchReference).subscribe({
      next: (claim) => this.trackedClaim.set(claim),
      error: (err) => {
        this.trackedClaim.set(null);
        this.errorMsg.set(err.error?.detail || 'Claim reference not found.');
      }
    });
  }

  submitNewClaim(): void {
    this.errorMsg.set('');
    this.claimService.submitClaim(this.newClaim).subscribe({
      next: (created) => {
        this.submissionSuccessRef.set(created.claimReference);
        this.searchReference = created.claimReference;
        this.trackedClaim.set(created);
      },
      error: (err) => this.errorMsg.set(err.error?.detail || 'Validation failed. Check all fields.')
    });
  }

  submitRequestedInfo(reference: string): void {
    if (!this.infoResponseText.trim()) return;
    this.claimService.submitAdditionalInfo(reference, this.infoResponseText).subscribe({
      next: (updated) => {
        this.infoResponseText = '';
        this.trackedClaim.set(updated);
      },
      error: (err) => this.errorMsg.set(err.error?.detail || 'Could not submit info.')
    });
  }
}
