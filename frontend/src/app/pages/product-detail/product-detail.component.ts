import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Product } from '../../models';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);

  product = signal<Product | null>(null);
  editing = signal(false);
  editName = signal('');
  editSerial = signal('');
  loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getById(id).subscribe({
        next: (p) => {
          this.product.set(p);
          this.editName.set(p.productName);
          this.editSerial.set(p.productSerialNumber);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        }
      });
    }
  }

  startEdit(): void {
    this.editing.set(true);
  }

  cancelEdit(): void {
    const p = this.product();
    if (p) {
      this.editName.set(p.productName);
      this.editSerial.set(p.productSerialNumber);
    }
    this.editing.set(false);
  }

  saveEdit(): void {
    const p = this.product();
    if (!p) return;

    const updated: Product = {
      ...p,
      productName: this.editName(),
      productSerialNumber: this.editSerial()
    };

    this.productService.update(updated).subscribe({
      next: (result) => {
        this.product.set(result);
        this.editing.set(false);
      },
      error: (err) => console.error('Update failed', err)
    });
  }

  deleteProduct(): void {
    const p = this.product();
    if (!p?.productId) return;

    if (confirm('Are you sure you want to delete this product?')) {
      this.productService.delete(p.productId).subscribe({
        next: () => this.router.navigate(['/']),
        error: (err) => console.error('Delete failed', err)
      });
    }
  }
}

