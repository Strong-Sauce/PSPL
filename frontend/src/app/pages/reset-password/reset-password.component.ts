import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css',
})
export class ResetPasswordComponent {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // Read the token from URL query param: /reset-password?token=abc123
  token = signal(this.route.snapshot.queryParamMap.get('token') ?? '');
  newPassword = signal('');
  submitting = signal(false);
  message = signal('');
  isError = signal(false);

  onSubmit(): void {
    if (!this.token().trim() || !this.newPassword().trim()) {
      this.isError.set(true);
      this.message.set('Token and new password are required.');
      return;
    }

    this.submitting.set(true);
    this.message.set('');

    this.authService
      .resetPassword({ token: this.token(), newPassword: this.newPassword() })
      .subscribe({
        next: (res) => {
          this.submitting.set(false);
          this.isError.set(false);
          this.message.set(res.message);
          // Redirect to login after 2 seconds
          setTimeout(() => void this.router.navigate(['/login']), 2000);
        },
        error: (err) => {
          this.submitting.set(false);
          this.isError.set(true);
          this.message.set(err?.error?.message ?? 'Failed to reset password. Please try again.');
        },
      });
  }
}

