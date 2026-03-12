import { Component, ChangeDetectorRef } from '@angular/core';
import { ApiService, Account } from '../../services/api.service';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-operations',
  standalone: true,
  imports: [NgIf, MatButtonModule, MatTableModule],
  templateUrl: './operations.html',
  styleUrls: ['./operations.css']
})
export class Operations {
  nonFollowers: Account[] = [];
  pendingRequests: Account[] = [];
  message: string = '';

  displayedColumns: string[] = ['username', 'profileUrl', 'timestamp'];

  constructor(private apiService: ApiService, private cd: ChangeDetectorRef) {}

  loadNonFollowers() {
    this.apiService.getNonFollowers().subscribe({
      next: (data) => {
        this.nonFollowers = data;
        this.pendingRequests = [];
        this.message = data.length === 0 ? 'No non-followers found.' : '';
        this.cd.detectChanges();
      },
      error: (err) => {
        this.message = err.error;
        this.cd.detectChanges();
      }
    });
  }

  loadPendingRequests() {
    this.apiService.getPendingRequests().subscribe({
      next: (data) => {
        this.pendingRequests = data;
        this.nonFollowers = [];
        this.message = data.length === 0 ? 'No pending requests found.' : '';
        this.cd.detectChanges();
      },
      error: (err) => {
        this.message = err.error;
        this.cd.detectChanges();
      }
    });
  }

  formatDate(timestamp?: number): string {
    if (!timestamp) return '';
    return new Date(timestamp * 1000).toLocaleString();
  }
}