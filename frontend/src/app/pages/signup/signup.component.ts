import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.css',
})
export class SignupComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  name = signal('');
  email = signal('');
  password = signal('');
  submitting = signal(false);
  errorMessage = signal('');

  onSubmit(): void {
    if (!this.name().trim() || !this.email().trim() || !this.password().trim()) {
      this.errorMessage.set('All fields are required.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    this.authService
      .signup({ name: this.name(), email: this.email(), password: this.password() })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/']);
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err?.error?.message ?? 'Signup failed. Please try again.');
        },
      });
  }
}

