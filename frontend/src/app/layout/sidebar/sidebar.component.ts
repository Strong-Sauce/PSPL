import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Product, Warranty } from '../../models';
import { ProductService } from '../../services/product.service';
import { WarrantyService } from '../../services/warranty.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {
  collapsed = input<boolean>(false);

  private readonly productService = inject(ProductService);
  private readonly warrantyService = inject(WarrantyService);

  products = signal<Product[]>([]);
  warranties = signal<Warranty[]>([]);
  productsOpen = signal(true);
  warrantiesOpen = signal(true);

  ngOnInit(): void {
    this.loadProducts();
    this.loadWarranties();
  }

  loadProducts(): void {
    this.productService.getAll().subscribe({
      next: (data) => this.products.set(data),
      error: (err) => console.error('Failed to load products', err)
    });
  }

  loadWarranties(): void {
    this.warrantyService.getExpiringSoon().subscribe({
      next: (data) => this.warranties.set(data),
      error: (err) => console.error('Failed to load warranties', err)
    });
  }

  toggleProducts(): void {
    this.productsOpen.update(v => !v);
  }

  toggleWarranties(): void {
    this.warrantiesOpen.update(v => !v);
  }
}

