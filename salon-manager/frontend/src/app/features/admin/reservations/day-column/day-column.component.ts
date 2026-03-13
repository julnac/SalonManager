import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReservationDetailDto } from '../../../../core/models/reservation.model';

@Component({
  selector: 'app-day-column',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './day-column.component.html',
  styleUrl: './day-column.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayColumnComponent {
  public date = input.required<Date>();
  public reservations = input.required<ReservationDetailDto[]>();
  public hours = input.required<number[]>();
  public isToday = input<boolean>(false);

  public approve = output<ReservationDetailDto>();
  public cancelReservation = output<ReservationDetailDto>();

  private readonly MIN_HEIGHT = 80;

  public getReservationStyle(reservation: ReservationDetailDto): Record<string, string> {
    const start = new Date(reservation.startTime);
    const end = new Date(reservation.endTime);

    const startHour = start.getHours() + start.getMinutes() / 60;
    const endHour = end.getHours() + end.getMinutes() / 60;

    const top = (startHour - 8) * 60;
    const naturalHeight = (endHour - startHour) * 60;

    return {
      top: `${top}px`,
      minHeight: `${Math.max(naturalHeight, this.MIN_HEIGHT)}px`
    };
  }

  public isShortReservation(reservation: ReservationDetailDto): boolean {
    const start = new Date(reservation.startTime);
    const end = new Date(reservation.endTime);
    const durationMinutes = (end.getTime() - start.getTime()) / (1000 * 60);

    return durationMinutes < 45;
  }

  public getStatusClass(status: ReservationDetailDto['status']): string {
    const classes: Record<ReservationDetailDto['status'], string> = {
      CREATED: 'status-created',
      APPROVED_BY_SALON: 'status-approved',
      CONFIRMED_BY_CLIENT: 'status-confirmed',
      CANCELLED: 'status-cancelled'
    };

    return classes[status];
  }

  public getStatusLabel(status: ReservationDetailDto['status']): string {
    const labels: Record<ReservationDetailDto['status'], string> = {
      CREATED: 'Nowa',
      APPROVED_BY_SALON: 'Zatwierdzona',
      CONFIRMED_BY_CLIENT: 'Potwierdzona',
      CANCELLED: 'Anulowana'
    };

    return labels[status];
  }

  public formatTime(dateStr: string): string {
    const date = new Date(dateStr);

    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
  }

  public formatDayName(): string {
    return this.date().toLocaleDateString('pl-PL', { weekday: 'short' });
  }

  public formatDayNumber(): string {
    return this.date().getDate().toString();
  }

  public onApprove(reservation: ReservationDetailDto, event: Event): void {
    event.stopPropagation();
    this.approve.emit(reservation);
  }

  public onCancel(reservation: ReservationDetailDto, event: Event): void {
    event.stopPropagation();
    this.cancelReservation.emit(reservation);
  }
}
