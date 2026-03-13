export interface ReservationDetailDto {
  reservationId: number;
  startTime: string; // ISO date-time
  endTime: string; // ISO date-time
  status: 'CREATED' | 'CONFIRMED_BY_CLIENT' | 'APPROVED_BY_SALON' | 'CANCELLED';
  totalPrice: number;
  clientFirstName: string;
  clientLastName: string;
  clientEmail: string;
  employeeFirstName: string;
  employeeLastName: string;
  serviceIds: number[];
}

export interface CreateReservationRequest {
  startTime: string; // ISO date-time
  employeeId: number;
  serviceIds: number[];
}

export interface UpdateReservationRequest {
  startTime: string; // ISO date-time
  employeeId: number;
  serviceIds: number[];
}
