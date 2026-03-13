import { Component, computed, EventEmitter, input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ReservationDetailDto } from '../../../../core/models/reservation.model';
import { OfferDto } from '../../../../core/models/offer.model';

export interface ReservationWithServices extends ReservationDetailDto {
  serviceDetails: OfferDto[];
}

@Component({
  selector: 'app-list-view',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule
  ],
  templateUrl: './list-view.component.html',
  styleUrl: './list-view.component.scss'
})
export class ListViewComponent {
  public reservations = input.required<ReservationWithServices[]>();

  @Output() public approve = new EventEmitter<ReservationDetailDto>();
  @Output() public cancelReservation = new EventEmitter<ReservationDetailDto>();

  public currentPage = signal(1);
  public pageSize = signal(10);
  public readonly pageSizeOptions = [5, 10, 25, 50] as const;

  public paginatedData = computed(() => {
    const data = this.reservations();
    const start = (this.currentPage() - 1) * this.pageSize();

    return data.slice(start, start + this.pageSize());
  });

  public totalPages = computed(() => Math.ceil(this.reservations().length / this.pageSize()) || 1);

  public onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  public previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  public nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }

  public onApprove(reservation: ReservationDetailDto): void {
    this.approve.emit(reservation);
  }

  public onCancel(reservation: ReservationDetailDto): void {
    this.cancelReservation.emit(reservation);
  }

  public formatDate(dateStr: string): string {
    const date = new Date(dateStr);

    return date.toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  public formatTime(dateStr: string): string {
    const date = new Date(dateStr);

    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
  }

  public getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED_BY_SALON': return 'status-approved';
      case 'CONFIRMED_BY_CLIENT': return 'status-confirmed';
      case 'CANCELLED': return 'status-cancelled';
      default: return 'status-created';
    }
  }

  public getStatusLabel(status: string): string {
    switch (status) {
      case 'CREATED': return 'Nowa';
      case 'APPROVED_BY_SALON': return 'Zatwierdzona';
      case 'CONFIRMED_BY_CLIENT': return 'Potwierdzona';
      case 'CANCELLED': return 'Anulowana';
      default: return status;
    }
  }
}
