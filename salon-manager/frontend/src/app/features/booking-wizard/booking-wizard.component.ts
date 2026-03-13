import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { finalize } from 'rxjs';
import { format } from 'date-fns';
import { BookingService } from './booking.service';
import { BookingComponent } from './booking/booking.component';
import { BookingSummaryComponent } from './booking-summary/booking-summary.component';
import { OfferComponent } from './offer/offer.component';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-booking-wizard',
  standalone: true,
  imports: [MatStepperModule, OfferComponent, BookingComponent, BookingSummaryComponent],
  templateUrl: './booking-wizard.component.html',
  styleUrl: './booking-wizard.component.scss',
})
export class BookingWizardComponent {
  protected readonly bookingService = inject(BookingService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  public readonly isSubmitting = signal(false);

  public finish(): void {
    const slot = this.bookingService.selectedSlot();
    if (!slot) return;

    this.isSubmitting.set(true);

    this.bookingService.createReservation({
      startTime: format(slot.date, 'yyyy-MM-dd') + 'T' + slot.time + ':00',
      employeeId: slot.employee.employeeId,
      serviceIds: this.bookingService.selectedOfferIds()
    }).pipe(
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: () => {
        this.notify.success('Rezerwacja utworzona!');
        this.bookingService.reset();
        void this.router.navigate(['/user/dashboard']);
      },
      error: (err: { status: number }) => {
        this.notify.error(err.status === 409 ? 'Termin zajęty!' : 'Błąd rezerwacji');
      }
    });
  }
}
