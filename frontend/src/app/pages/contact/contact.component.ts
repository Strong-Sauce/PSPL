import { Component } from '@angular/core';

@Component({
  selector: 'app-contact',
  standalone: true,
  template: `
    <div class="page-container">
      <h1>Contact Us</h1>
      <p>Have questions? Reach out to the PSPL support team.</p>
      <div class="contact-info">
        <div class="contact-row">
          <span class="label">Email</span>
          <span>support&#64;pspl.com</span>
        </div>
        <div class="contact-row">
          <span class="label">Phone</span>
          <span>+91-XXXX-XXXXXX</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-container { max-width: 640px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 1.5rem; font-weight: 700; color: #1a1a2e; margin-bottom: 16px; }
    p { color: #555; line-height: 1.7; font-size: 0.95rem; margin-bottom: 24px; }
    .contact-info { background: #fff; border-radius: 14px; padding: 20px; border: 1px solid #f0f0f0; }
    .contact-row { display: flex; padding: 12px 0; border-bottom: 1px solid #f5f5f5; font-size: 0.9rem; }
    .contact-row:last-child { border-bottom: none; }
    .label { width: 100px; font-weight: 600; color: #888; }
  `]
})
export class ContactComponent {}

