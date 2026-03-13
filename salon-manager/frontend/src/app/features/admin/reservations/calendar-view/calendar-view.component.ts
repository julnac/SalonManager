import { Component, computed, EventEmitter, input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReservationDetailDto } from '../../../../core/models/reservation.model';
import { DayColumnComponent } from '../day-column/day-column.component';

@Component({
  selector: 'app-calendar-view',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    DayColumnComponent
  ],
  templateUrl: './calendar-view.component.html',
  styleUrl: './calendar-view.component.scss'
})
export class CalendarViewComponent {
  public reservations = input.required<ReservationDetailDto[]>();

  @Output() public approve = new EventEmitter<ReservationDetailDto>();
  @Output() public cancelReservation = new EventEmitter<ReservationDetailDto>();

  public readonly hours = Array.from({ length: 12 }, (_, i) => i + 8);
  public weekStartDate = signal(this.getMonday(new Date()));

  public weekDays = computed(() => {
    const start = this.weekStartDate();

    return Array.from({ length: 6 }, (_, i) => {
      const date = new Date(start);
      date.setDate(start.getDate() + i);

      return date;
    });
  });

  public reservationsByDay = computed(() => {
    const days = this.weekDays();
    const reservations = this.reservations();

    return days.map((day) => {
      const dayStr = this.formatDateKey(day);

      return reservations.filter((r) => {
        const rDate = this.formatDateKey(new Date(r.startTime));

        return rDate === dayStr;
      });
    });
  });

  public previousWeek(): void {
    const current = this.weekStartDate();
    const prev = new Date(current);
    prev.setDate(current.getDate() - 7);
    this.weekStartDate.set(prev);
  }

  public nextWeek(): void {
    const current = this.weekStartDate();
    const next = new Date(current);
    next.setDate(current.getDate() + 7);
    this.weekStartDate.set(next);
  }

  public goToToday(): void {
    this.weekStartDate.set(this.getMonday(new Date()));
  }

  public formatMonthYear(): string {
    const start = this.weekStartDate();
    const end = new Date(start);
    end.setDate(start.getDate() + 6);

    if (start.getMonth() === end.getMonth()) {
      return start.toLocaleDateString('pl-PL', { month: 'long', year: 'numeric' });
    }

    return `${start.toLocaleDateString('pl-PL', { month: 'short' })} - ${end.toLocaleDateString('pl-PL', { month: 'short', year: 'numeric' })}`;
  }

  public isToday(date: Date): boolean {
    const today = new Date();

    return this.formatDateKey(date) === this.formatDateKey(today);
  }

  public onApprove(reservation: ReservationDetailDto): void {
    this.approve.emit(reservation);
  }

  public onCancel(reservation: ReservationDetailDto): void {
    this.cancelReservation.emit(reservation);
  }

  private getMonday(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    d.setHours(0, 0, 0, 0);

    return d;
  }

  private formatDateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
