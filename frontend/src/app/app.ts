import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { NavbarComponent } from './layout/navbar/navbar.component';
import { SidebarComponent } from './layout/sidebar/sidebar.component';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  sidebarCollapsed = signal(false);

  // Track the current URL so we can hide sidebar on auth pages
  currentPath = signal(this.router.url);

  // True if user is logged in
  isAuthenticated = this.authService.isAuthenticated;

  // Show sidebar only when logged in AND NOT on an auth page
  showSidebar = computed(() => {
    const path = this.currentPath();
    const onAuthPage = path.startsWith('/login')
      || path.startsWith('/signup')
      || path.startsWith('/forgot-password')
      || path.startsWith('/reset-password');
    return this.isAuthenticated() && !onAuthPage;
  });

  constructor() {
    // Restore session on app startup (handles browser refresh)
    this.authService.initSession();

    // Update currentPath whenever the route changes
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.currentPath.set(event.urlAfterRedirects);
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }
}
