import { Component, ChangeDetectorRef } from '@angular/core';
import { ApiService } from '../../services/api.service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [NgIf, MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './upload.html',
  styleUrls: ['./upload.css']
})
export class Upload {
  message: string = '';
  loading: boolean = false;

  constructor(private apiService: ApiService, private cd: ChangeDetectorRef) {}

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.loading = true;
      this.apiService.uploadFile(file).subscribe({
        next: (res) => {
          this.message = res;
          this.loading = false;
          this.cd.detectChanges(); // force Angular to refresh view
        },
        error: () => {
          this.message = 'Upload failed';
          this.loading = false;
          this.cd.detectChanges();
        }
      });
    }
  }
}