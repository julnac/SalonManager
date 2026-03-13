import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from '../services/api.service';
import { CreateReservationRequest, ReservationDetailDto } from '../models/reservation.model';
import { AvailabilityResponseDto } from '../models/availability.model';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ReservationsService {
  private readonly api = inject(ApiService);

  private readonly reservationsSignal = signal<ReservationDetailDto[]>([]);
  public readonly reservations = this.reservationsSignal.asReadonly();

  private readonly myReservationsSignal = signal<ReservationDetailDto[]>([]);
  public readonly myReservations = this.myReservationsSignal.asReadonly();

  private readonly avaliabilitySignal = signal<AvailabilityResponseDto>({} as AvailabilityResponseDto);
  public readonly availability = this.avaliabilitySignal.asReadonly();

  public loadReservations(status?: string): Observable<ReservationDetailDto[]> {
    const params = status ? `?status=${status}` : '';

    return this.api.get<ReservationDetailDto[]>(`reservations${params}`).pipe(
        tap((data) => this.reservationsSignal.set(data))
    );
  }

  public loadMyReservations(): Observable<ReservationDetailDto[]> {
    return this.api.get<ReservationDetailDto[]>('reservations/my').pipe(
      tap((data) => this.myReservationsSignal.set(data))
    );
  }

  public createReservation(request: CreateReservationRequest): Observable<ReservationDetailDto> {
    return this.api.post<ReservationDetailDto>('reservations', request);
  }

  public approveReservation(id: number): Observable<ReservationDetailDto> {
    return this.api.put<ReservationDetailDto>(`reservations/${id}/approve`, {}).pipe(
      tap((updatedReservation) => this.reservationsSignal.update((reservations) =>
          reservations.map((reservation) => reservation.reservationId === id ? updatedReservation : reservation))
      ));
  }

  public cancelReservation(id: number): Observable<ReservationDetailDto> {
    return this.api.put<ReservationDetailDto>(`reservations/${id}/cancel`, {}).pipe(
      tap(() => this.reservationsSignal.update((reservations) => reservations.filter((reservation) => reservation.reservationId !== id)))
    );
  }

  public confirmReservationByClient(reservationId: number): Observable<ReservationDetailDto> {
    return this.api.put<ReservationDetailDto>(`reservations/${reservationId}/confirm`, {}).pipe(
      tap((updatedReservation) => this.reservationsSignal.update((reservations) =>
          reservations.map((reservation) => reservation.reservationId === reservationId ? updatedReservation : reservation))
      ));
  }

  public checkAvailability(date: string, serviceIds: number[]): Observable<AvailabilityResponseDto> {
    let params = new HttpParams().set('date', date);
    if (serviceIds.length > 0) {
      params = params.set('serviceIds', serviceIds.join(','));
    }
    
    return this.api.get<AvailabilityResponseDto>('reservations/availability', params).pipe(
      tap((data) => this.avaliabilitySignal.set(data))
    );
  } 
}