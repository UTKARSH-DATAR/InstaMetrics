import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Account {
  username: string;
  profileUrl: string;
  timestamp?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  // In production on Render, frontend and backend are served from the same origin,
  // so we can use a simple relative base path.
  private baseUrl = '/instametrics';

  constructor(private http: HttpClient) {}

  uploadFile(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.baseUrl}/upload`, formData, { responseType: 'text', withCredentials: true });
  }

  getNonFollowers(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/nonfollowers`, { withCredentials: true });
  }

  getPendingRequests(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/pendingRequests`, { withCredentials: true });
  }
}