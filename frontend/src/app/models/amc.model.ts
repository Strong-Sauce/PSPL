import { AMCOffer } from './amc-offer.model';

export interface AMC {
  amcId?: string;
  amcStartDate: string; // ISO date string (LocalDate)
  amcEndDate: string;   // ISO date string (LocalDate)
  amcOfferList?: AMCOffer[];
}

