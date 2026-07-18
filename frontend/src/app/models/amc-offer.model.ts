export interface AMCOffer {
  offerId?: string;
  offerType: string;            // Silver / Gold
  offerDurationMonths: number;  // Java Integer
  offerPrice: number;           // Java Double
  offerTerms: string;
}

