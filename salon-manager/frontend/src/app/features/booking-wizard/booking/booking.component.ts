import { Component, OnInit, computed, inject, Output, signal, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { isSameDay, format } from 'date-fns';
import { forkJoin } from 'rxjs';
import { BookingService } from '../booking.service';
import { NotificationService } from '../../../core/services/notification.service';
import { EmployeeAvailabilityDto } from '../../../core/models/availability.model';
import { DayAvailability } from './booking.interface';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './booking.component.html',
  styleUrl: './booking.component.scss'
})
export class BookingComponent implements OnInit {
  @Output() public back = new EventEmitter<void>();
  @Output() public next = new EventEmitter<void>();

  protected readonly store = inject(BookingService);
  private readonly notify = inject(NotificationService);

  public readonly isLoading = signal(true);
  public readonly employeeFilter = signal<number | null>(null);

  public readonly allEmployees = computed(() => {
    const allEmps = this.store.days()
      .flatMap((day) => day.availability?.employees ?? [])
      .map((emp) => ({ employeeId: emp.employeeId, firstName: emp.firstName, lastName: emp.lastName }));

    const unique = new Map(allEmps.map((e) => [e.employeeId, e]));

    return Array.from(unique.values());
  });

  public readonly filteredEmployees = computed(() => {
    const day = this.store.selectedDay();
    if (!day?.availability) return [];

    const now = new Date();
    const isToday = isSameDay(day.date, now);
    const currentTime = format(now, 'HH:mm');

    return day.availability.employees
      .filter((e) => this.employeeFilter() === null || e.employeeId === this.employeeFilter())
      .map((employee) => ({
        ...employee,
        availableSlots: isToday
          ? employee.availableSlots.filter((slot) => slot.substring(0, 5) > currentTime)
          : employee.availableSlots
      }))
      .filter((e) => e.availableSlots.length > 0);
  });

  public ngOnInit(): void {
    this.loadAvailability();

  }

  private loadAvailability(): void {
    if (this.store.selectedOffers().length === 0) {
      this.isLoading.set(false);

      return;
    }

    this.isLoading.set(true);

    const requests = this.store.bookingDays().map((day) =>
      this.store.getAvailability(day.dateStr)
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        const days: DayAvailability[] = this.store.bookingDays().map((day, i) => ({
          ...day,
          availability: results[i],
          isLoading: false
        }));

        this.store.days.set(days);

        const firstAvailable = days.find((d) => this.hasAvailableSlots(d));
        if (firstAvailable && !this.store.selectedDay()) {
          this.store.selectedDay.set(firstAvailable);
        }

        this.isLoading.set(false);
      },
      error: () => {
        this.notify.error('Nie udało się pobrać dostępności');
        this.isLoading.set(false);
      }
    });
  }

  public selectDay(day: DayAvailability): void {
    this.store.selectedDay.set(day);
    this.store.selectedSlot.set(null);
  }

  public selectSlot(employee: EmployeeAvailabilityDto, slot: string): void {
    const day = this.store.selectedDay();
    if (!day) return;

    this.store.selectedSlot.set({
      date: day.date,
      time: slot.substring(0, 5),
      employee
    });
  }

  public hasAvailableSlots(day: DayAvailability): boolean {
    return day.availability?.employees.some((e) => e.availableSlots.length > 0) ?? false;
  }

  public formatSlotTime(slot: string): string {
    return slot.substring(0, 5);
  }

  public proceed(): void {
    if (this.store.selectedSlot()) {
      this.next.emit();
    }
  }
}
