import { Sale } from './sale.model';

export interface Customer {
  custId?: string;
  custName: string;
  purchases?: Sale[];
}

