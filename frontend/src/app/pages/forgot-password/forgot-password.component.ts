import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css',
})
export class ForgotPasswordComponent {
  private readonly authService = inject(AuthService);

  email = signal('');
  submitting = signal(false);
  message = signal('');       // Can be success or error
  isError = signal(false);    // Controls which alert style is shown

  onSubmit(): void {
    if (!this.email().trim()) {
      this.isError.set(true);
      this.message.set('Please enter your email address.');
      return;
    }

    this.submitting.set(true);
    this.message.set('');

    this.authService.forgotPassword({ email: this.email() }).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.isError.set(false);
        this.message.set(res.message); // "Password reset link has been sent..."
      },
      error: (err) => {
        this.submitting.set(false);
        this.isError.set(true);
        this.message.set(err?.error?.message ?? 'Something went wrong. Please try again.');
      },
    });
  }
}

