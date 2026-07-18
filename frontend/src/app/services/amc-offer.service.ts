import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AMCOffer } from '../models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AmcOfferService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/amc-offers`;

  getAll(): Observable<AMCOffer[]> {
    return this.http.get<AMCOffer[]>(this.baseUrl);
  }

  getById(id: string): Observable<AMCOffer> {
    return this.http.get<AMCOffer>(`${this.baseUrl}/${id}`);
  }

  create(offer: AMCOffer): Observable<AMCOffer> {
    return this.http.post<AMCOffer>(this.baseUrl, offer);
  }

  update(offer: AMCOffer): Observable<AMCOffer> {
    return this.http.put<AMCOffer>(this.baseUrl, offer);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

