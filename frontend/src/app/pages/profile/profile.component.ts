import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * ProfileComponent shows the logged-in user's information and a logout button.
 * The user data comes from the AuthService signal (set during login or session restore).
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page-container">
      <h1>Profile</h1>

      @if (currentUser(); as user) {
        <div class="profile-card">
          <!-- Avatar icon -->
          <div class="avatar">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>

          <!-- User info rows -->
          <div class="info">
            <p class="info-row">
              <span class="label">Name</span>
              <span>{{ user.name }}</span>
            </p>
            <p class="info-row">
              <span class="label">Email</span>
              <span>{{ user.email }}</span>
            </p>
            <p class="info-row">
              <span class="label">User ID</span>
              <span class="user-id">{{ user.id }}</span>
            </p>
          </div>

          <!-- Actions -->
          <div class="actions">
            <button class="btn-danger" (click)="onLogout()">
              Logout
            </button>
            <a routerLink="/" class="btn-secondary">Back to Home</a>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 640px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 1.5rem; font-weight: 700; color: #1a1a2e; margin-bottom: 24px; }

    .profile-card {
      background: #fff;
      border-radius: 14px;
      padding: 28px;
      border: 1px solid #f0f0f0;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    }

    .avatar {
      width: 80px; height: 80px; border-radius: 50%;
      background: rgba(78, 204, 163, 0.1); color: #4ecca3;
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 20px;
    }

    .info-row {
      display: flex; padding: 12px 0;
      border-bottom: 1px solid #f5f5f5; margin: 0; font-size: 0.9rem;
    }
    .info-row:last-child { border-bottom: none; }
    .label { width: 100px; font-weight: 600; color: #888; flex-shrink: 0; }
    .user-id { font-family: monospace; font-size: 0.8rem; color: #999; word-break: break-all; }

    .actions { display: flex; gap: 12px; margin-top: 24px; flex-wrap: wrap; }

    .btn-danger {
      padding: 10px 20px; background: #e94560; color: #fff;
      border: none; border-radius: 10px; font-size: 0.875rem;
      font-weight: 600; cursor: pointer; transition: background 0.2s;
    }
    .btn-danger:hover { background: #c73e55; }

    .btn-secondary {
      padding: 10px 20px; background: #f0f0f0; color: #555;
      border-radius: 10px; font-size: 0.875rem; font-weight: 600;
      text-decoration: none; transition: background 0.2s;
    }
    .btn-secondary:hover { background: #e5e5e5; }
  `]
})
export class ProfileComponent {
  private readonly authService = inject(AuthService);

  // Reactive signal — updates automatically when user logs in/out
  currentUser = this.authService.currentUser;

  onLogout(): void {
    this.authService.logout().subscribe();
  }
}
