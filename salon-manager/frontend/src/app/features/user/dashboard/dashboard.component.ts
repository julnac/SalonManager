import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { DashboardService } from './dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ReservationDetailDto } from '../../../core/models/reservation.model';
import { finalize } from 'rxjs';
import { ReservationUtilityService } from '../../admin/reservations/reservation-utility.service';
import { DialogService } from '../../../shared/components/confirmation-dialog/dialog.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  protected readonly dashboardService = inject(DashboardService);
  protected readonly authService = inject(AuthService);
  protected readonly reservationUtils = inject(ReservationUtilityService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialogService = inject(DialogService);

  public isLoading = signal(true);

  public ngOnInit(): void {
    this.refreshData();
  }

  public refreshData(): void {
    this.isLoading.set(true);
    this.dashboardService.loadMyReservations()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        error: () => this.notificationService.error('Błąd pobierania danych')
      });
  }

  public onCancel(reservation: ReservationDetailDto): void {
    this.dialogService.confirm({
      title: 'Anulowanie rezerwacji',
      message: `Czy na pewno chcesz anulować wizytę?`,
      confirmText: 'Tak, anuluj',
      type: 'danger'
    }).subscribe((confirmed) => {
      if (confirmed) {
        this.dashboardService.cancelReservation(reservation.reservationId).subscribe({
          next: () => this.notificationService.success('Rezerwacja została anulowana'),
          error: () => this.notificationService.error('Nie udało się anulować rezerwacji')
        });
      }
    });
  }

  public onConfirm(reservation: ReservationDetailDto): void {
    if (!confirm('Czy na pewno chcesz potwierdzić tę rezerwację?')) return;

    this.dashboardService.confirmReservation(reservation.reservationId).subscribe({
      next: () => this.notificationService.success('Rezerwacja została potwierdzona'),
      error: () => this.notificationService.error('Nie udało się potwierdzić rezerwacji')
    });
  }

  public canCancel(reservation: ReservationDetailDto): boolean {
    const now = new Date();
    const startTime = new Date(reservation.startTime);

    return reservation.status !== 'CANCELLED' && startTime > now;
  }

  public canConfirm(reservation: ReservationDetailDto): boolean {
    return reservation.status === 'APPROVED_BY_SALON';
  }
}
