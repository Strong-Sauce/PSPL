import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Product } from '../../models';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-create',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './product-create.component.html',
  styleUrl: './product-create.component.css'
})
export class ProductCreateComponent {
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);

  productName = signal('');
  productSerialNumber = signal('');
  submitting = signal(false);
  errorMsg = signal('');

  onSubmit(): void {
    if (!this.productName() || !this.productSerialNumber()) {
      this.errorMsg.set('Please fill in all required fields.');
      return;
    }

    this.submitting.set(true);
    this.errorMsg.set('');

    const product: Product = {
      productName: this.productName(),
      productSerialNumber: this.productSerialNumber()
    };

    this.productService.create(product).subscribe({
      next: (created) => {
        this.router.navigate(['/products', created.productId]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMsg.set('Failed to create product. Please try again.');
        console.error(err);
      }
    });
  }
}

