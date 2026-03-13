import { Injectable, computed, inject, signal } from '@angular/core';
import { BookingDay, DayAvailability, SelectedSlot } from './booking/booking.interface';
import { ReservationsService } from '../../core/services/reservations.service';
import {
  addDays,
  format,
  startOfDay
} from 'date-fns';
import { pl } from 'date-fns/locale';
import { OfferService } from '../../core/services/offer.service';
import { OfferDto } from '../../core/models/offer.model';
import { Observable } from 'rxjs/internal/Observable';
import { ReservationDetailDto, CreateReservationRequest } from '../../core/models/reservation.model';
import { AvailabilityResponseDto } from '../../core/models/availability.model';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private readonly offerService = inject(OfferService);
  private readonly reservationsService = inject(ReservationsService);

  public readonly selectedOffers = signal<OfferDto[]>([]);
  public readonly selectedDay = signal<DayAvailability | null>(null);
  public readonly selectedSlot = signal<SelectedSlot | null>(null);
  public readonly days = signal<DayAvailability[]>([]);

  public readonly selectedOfferIds = computed(() => this.selectedOffers().map((o) => o.id));
  public readonly totalDuration = computed(() => this.selectedOffers().reduce((sum, s) => sum + s.durationMinutes, 0));
  public readonly totalPrice = computed(() => this.selectedOffers().reduce((sum, s) => sum + s.price, 0));
  public readonly canProceedToBooking = computed(() => this.selectedOffers().length > 0);
  public readonly canProceedToSummary = computed(() => this.selectedSlot() !== null);

  public readonly bookingDays = computed((): BookingDay[] => {
    const today = startOfDay(new Date());

    return Array.from({ length: 7 }).map((_, i) => {
      const date = addDays(today, i);

      return {
        date,
        dateStr: format(date, 'yyyy-MM-dd'),
        dayNumber: format(date, 'd'),
        label: format(date, 'd MMM', { locale: pl }),
        dayName: format(date, 'EEEE', { locale: pl })
      };
    });
  });

  public getOffers(): Observable<OfferDto[]> {
    return this.offerService.loadOffers();
  }

  public createReservation(request: CreateReservationRequest): Observable<ReservationDetailDto> {
    return this.reservationsService.createReservation(request); 
  }
  
  public getAvailability(date: string): Observable<AvailabilityResponseDto> {
    return this.reservationsService.checkAvailability(date, this.selectedOfferIds());
  }

  public reset(): void {
    this.selectedOffers.set([]);
    this.selectedDay.set(null);
    this.selectedSlot.set(null);
    this.days.set([]);
  }
}