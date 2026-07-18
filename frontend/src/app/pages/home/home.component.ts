import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Product, Warranty } from '../../models';
import { ProductService } from '../../services/product.service';
import { WarrantyService } from '../../services/warranty.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly warrantyService = inject(WarrantyService);

  // Products
  allProducts = signal<Product[]>([]);
  productSearch = signal('');
  productPage = signal(0);
  readonly productPageSize = 6;

  filteredProducts = computed(() => {
    const term = this.productSearch().toLowerCase();
    return this.allProducts().filter(p =>
      p.productName.toLowerCase().includes(term) ||
      p.productSerialNumber.toLowerCase().includes(term)
    );
  });

  pagedProducts = computed(() => {
    const start = this.productPage() * this.productPageSize;
    return this.filteredProducts().slice(start, start + this.productPageSize);
  });

  totalProductPages = computed(() =>
    Math.ceil(this.filteredProducts().length / this.productPageSize)
  );

  // Warranties
  allWarranties = signal<Warranty[]>([]);
  warrantySearch = signal('');
  warrantyPage = signal(0);
  readonly warrantyPageSize = 6;

  filteredWarranties = computed(() => {
    const term = this.warrantySearch().toLowerCase();
    return this.allWarranties().filter(w =>
      w.warrantyEndDate.toLowerCase().includes(term) ||
      w.warrantyStartDate.toLowerCase().includes(term)
    );
  });

  pagedWarranties = computed(() => {
    const start = this.warrantyPage() * this.warrantyPageSize;
    return this.filteredWarranties().slice(start, start + this.warrantyPageSize);
  });

  totalWarrantyPages = computed(() =>
    Math.ceil(this.filteredWarranties().length / this.warrantyPageSize)
  );

  ngOnInit(): void {
    this.productService.getAll().subscribe({
      next: (data) => this.allProducts.set(data)
    });
    this.warrantyService.getExpiringSoon().subscribe({
      next: (data) => this.allWarranties.set(data)
    });
  }

  onProductSearch(term: string): void {
    this.productSearch.set(term);
    this.productPage.set(0);
  }

  onWarrantySearch(term: string): void {
    this.warrantySearch.set(term);
    this.warrantyPage.set(0);
  }

  setProductPage(page: number): void {
    this.productPage.set(page);
  }

  setWarrantyPage(page: number): void {
    this.warrantyPage.set(page);
  }
}

