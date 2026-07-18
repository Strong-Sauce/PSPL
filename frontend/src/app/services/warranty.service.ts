import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Warranty } from '../models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WarrantyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/warranty`;

  getExpiringSoon(): Observable<Warranty[]> {
    return this.http.get<Warranty[]>(this.baseUrl);
  }

  getByCustomerId(custId: string): Observable<Warranty[]> {
    return this.http.get<Warranty[]>(`${this.baseUrl}/${custId}`);
  }
}

