import { Component, inject, computed, Output, EventEmitter } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { BookingService } from '../booking.service';

@Component({
  selector: 'app-booking-summary',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatDividerModule, DecimalPipe, DatePipe],
  templateUrl: './booking-summary.component.html',
  styleUrl: './booking-summary.component.scss'
})
export class BookingSummaryComponent {
  protected readonly bookingService = inject(BookingService);

  @Output() public readonly confirm = new EventEmitter<void>();
  @Output() public readonly back = new EventEmitter<void>();

  public readonly slot = computed(() => this.bookingService.selectedSlot());
  public readonly offers = computed(() => this.bookingService.selectedOffers());

  public onConfirm(): void {
    this.confirm.emit();
  }
}