import { AMC } from './amc.model';

export interface Warranty {
  warrantyId?: string;
  warrantyStartDate: string; // ISO date string (LocalDate)
  warrantyEndDate: string;   // ISO date string (LocalDate)
  amcList?: AMC[];
}

