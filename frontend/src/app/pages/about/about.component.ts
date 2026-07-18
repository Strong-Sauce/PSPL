import { Component } from '@angular/core';

@Component({
  selector: 'app-about',
  standalone: true,
  template: `
    <div class="page-container">
      <h1>About Us</h1>
      <p>PSPL Post-Sale Product Lifecycle &amp; AMC Management System.</p>
      <p>This platform helps manage products, warranties, sales, and Annual Maintenance Contracts
         using a Neo4j graph database for rich relationship tracking across the entire product lifecycle.</p>
    </div>
  `,
  styles: [`
    .page-container { max-width: 640px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 1.5rem; font-weight: 700; color: #1a1a2e; margin-bottom: 16px; }
    p { color: #555; line-height: 1.7; font-size: 0.95rem; }
  `]
})
export class AboutComponent {}

