import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClaimantPortalComponent } from './components/claimant-portal.component';
import { StaffWorkspaceComponent } from './components/staff-workspace.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ClaimantPortalComponent, StaffWorkspaceComponent],
  template: `
    <header style="background:#0f172a; color:white; padding:0.9rem 1.5rem; display:flex; justify-content:space-between; align-items:center;">
      <div>
        <strong style="font-size:1.15rem; letter-spacing:0.04em;">CHUBB APAC</strong>
        <span style="color:#94a3b8; margin-left:0.75rem; font-size:0.9rem;">
          Unified Motor & Property Claims Platform (SG | HK | AU | MY | TH | JP)
        </span>
      </div>

      <div style="display:flex; gap:0.5rem;">
        <button
          [style.background]="activePersona() === 'STAFF' ? '#2563eb' : '#334155'"
          (click)="activePersona.set('STAFF')">
          Claims Staff & Manager Workspace
        </button>
        <button
          [style.background]="activePersona() === 'CLAIMANT' ? '#2563eb' : '#334155'"
          (click)="activePersona.set('CLAIMANT')">
          Customer Claimant Portal
        </button>
      </div>
    </header>

    <main class="container">
      @if (activePersona() === 'STAFF') {
        <app-staff-workspace />
      } @else {
        <app-claimant-portal />
      }
    </main>
  `
})
export class AppComponent {
  activePersona = signal<'STAFF' | 'CLAIMANT'>('STAFF');
}
