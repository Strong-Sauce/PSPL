import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, tap } from 'rxjs';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  ResetPasswordRequest,
  SignupRequest,
} from '../models/auth.model';
import { User } from '../models/user.model';
import { environment } from '../../environments/environment';

/**
 * AuthService is the single source of truth for authentication state in the frontend.
 *
 * - currentUser: Angular signal that holds the logged-in user (or null if guest)
 * - isAuthenticated: computed signal derived from currentUser
 * - All auth API calls go through this service
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // Base URL for auth endpoints (e.g. http://localhost:8080 in dev, empty in prod)
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  // Signal: holds the current user or null. Components read this reactively.
  currentUser = signal<User | null>(null);

  // Computed signal: true if a user is logged in
  isAuthenticated = computed(() => this.currentUser() !== null);

  // ─── SIGNUP ────────────────────────────────────────────────────────────────

  signup(payload: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/signup`, payload).pipe(
      tap((res) => this.currentUser.set(res.user)) // Set user signal on success
    );
  }

  // ─── LOGIN ─────────────────────────────────────────────────────────────────

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap((res) => this.currentUser.set(res.user)) // Set user signal on success
    );
  }

  // ─── CURRENT USER (session restore) ────────────────────────────────────────

  /**
   * Fetches the current user from the backend using the active session cookie.
   * Called on app startup to restore the session after a page refresh.
   */
  fetchCurrentUser(): Observable<User | null> {
    return this.http.get<User>(`${this.baseUrl}/me`).pipe(
      tap((user) => this.currentUser.set(user)),
      map((user) => user),
      catchError(() => {
        // 401 = not logged in, that's fine — just set user to null
        this.currentUser.set(null);
        return of(null);
      })
    );
  }

  /**
   * Called once on app startup in app.ts.
   * Restores session so a page refresh doesn't log the user out.
   */
  initSession(): void {
    this.fetchCurrentUser().subscribe();
  }

  // ─── FORGOT PASSWORD ───────────────────────────────────────────────────────

  forgotPassword(payload: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/forgot-password`, payload);
  }

  // ─── RESET PASSWORD ────────────────────────────────────────────────────────

  resetPassword(payload: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/reset-password`, payload);
  }

  // ─── LOGOUT ─────────────────────────────────────────────────────────────────

  /**
   * Calls the backend to destroy the session, then clears the frontend state.
   */
  logout(): Observable<void> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => {
        this.currentUser.set(null); // Clear user signal
        void this.router.navigate(['/login']);
      }),
      map(() => void 0),
      catchError(() => {
        // Even if the API call fails, clear local state and redirect
        this.currentUser.set(null);
        void this.router.navigate(['/login']);
        return of(void 0);
      })
    );
  }
}

