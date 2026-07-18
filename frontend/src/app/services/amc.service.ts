import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AMC } from '../models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AmcService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/amcs`;

  getAll(): Observable<AMC[]> {
    return this.http.get<AMC[]>(this.baseUrl);
  }

  getById(id: string): Observable<AMC> {
    return this.http.get<AMC>(`${this.baseUrl}/${id}`);
  }

  create(amc: AMC): Observable<AMC> {
    return this.http.post<AMC>(this.baseUrl, amc);
  }

  update(amc: AMC): Observable<AMC> {
    return this.http.put<AMC>(this.baseUrl, amc);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

