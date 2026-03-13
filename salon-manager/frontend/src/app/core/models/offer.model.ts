export interface OfferDto {
  id: number;
  name: string;
  price: number;
  durationMinutes: number;
}

export interface CreateOfferRequest {
  name: string;
  price: number; // required, > 0
  durationMinutes: number;
}

export interface UpdateOfferRequest {
  name: string;
  price: number; // required, > 0
  durationMinutes: number;
}
