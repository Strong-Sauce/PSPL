import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Form field signals
  email = signal('');
  password = signal('');
  submitting = signal(false);
  errorMessage = signal(''); // Shown in red below the form

  onSubmit(): void {
    // Client-side validation before sending to server
    if (!this.email().trim() || !this.password().trim()) {
      this.errorMessage.set('Email and password are required.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    this.authService.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigate(['/']); // Redirect to home on success
      },
      error: (err) => {
        this.submitting.set(false);
        // Show the server error message (e.g. "Invalid credentials")
        this.errorMessage.set(err?.error?.message ?? 'Login failed. Please try again.');
      },
    });
  }
}

