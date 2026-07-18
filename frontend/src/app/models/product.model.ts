import { Warranty } from './warranty.model';

export interface Product {
  productId?: string;
  productName: string;
  productSerialNumber: string;
  warrantyList?: Warranty[];
}

