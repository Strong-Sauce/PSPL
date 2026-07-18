import { Product } from './product.model';

export interface Sale {
  saleId?: string;
  saleDate: string; // ISO date string (LocalDate)
  productList?: Product[];
}

