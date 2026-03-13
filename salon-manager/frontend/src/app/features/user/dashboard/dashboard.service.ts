import { Injectable, computed, inject } from '@angular/core';
import { ReservationsService } from '../../../core/services/reservations.service';
import { Observable } from 'rxjs';
import { ReservationDetailDto } from '../../../core/models/reservation.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly reservationService = inject(ReservationsService);

  public readonly myUpcomingReservations = computed(() => {
    const now = new Date();

    return this.reservationService.myReservations()
      .filter((r) => new Date(r.startTime) >= now && r.status !== 'CANCELLED')
      .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  });

  public readonly myPastReservations = computed(() => {
    const now = new Date();

    return this.reservationService.myReservations()
      .filter((r) => new Date(r.startTime) < now && r.status !== 'CANCELLED')
      .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
  });

  public loadMyReservations(): Observable<ReservationDetailDto[]> {
    return this.reservationService.loadMyReservations();
  }

  public cancelReservation(id: number): Observable<ReservationDetailDto> {
    return this.reservationService.cancelReservation(id);
  }

  public confirmReservation(id: number): Observable<ReservationDetailDto> {
    return this.reservationService.confirmReservationByClient(id);
  }
}
