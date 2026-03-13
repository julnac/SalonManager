import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { shareReplay, switchMap, tap } from 'rxjs/operators';
import { ReservationsService } from '../../../core/services/reservations.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ReservationDetailDto } from '../../../core/models/reservation.model';
import { OfferDto } from '../../../core/models/offer.model';
import { CalendarViewComponent } from './calendar-view/calendar-view.component';
import { ListViewComponent, ReservationWithServices } from './list-view/list-view.component';
import { ConfirmationDialogData } from '../../../shared/components/confirmation-dialog/confirmation-dialog.interface';
import { OfferService } from '../../../core/services/offer.service';

interface LoadedData {
  reservations: ReservationDetailDto[];
  services: OfferDto[];
}

@Component({
  selector: 'app-reservations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatCheckboxModule,
    MatDialogModule,
    CalendarViewComponent,
    ListViewComponent
  ],
  templateUrl: './reservations.component.html',
  styleUrl: './reservations.component.scss'
})
export class ReservationsComponent {
  private readonly reservationsService = inject(ReservationsService);
  private readonly notificationService = inject(NotificationService);
  protected readonly offerService = inject(OfferService);
  private readonly dialog = inject(MatDialog);

  public activeView = signal<'calendar' | 'list'>('calendar');

  private readonly refreshTrigger = signal<void>(undefined);

  public isLoading = signal(true);

  private readonly rawData$ = toObservable(this.refreshTrigger).pipe(
    tap(() => this.isLoading.set(true)),
    switchMap(() => forkJoin({
      reservations: this.reservationsService.loadReservations(),
      services: this.offerService.loadOffers()
    })),
    tap(() => this.isLoading.set(false)),
    shareReplay(1)
  );

  private readonly loadedData = toSignal(this.rawData$, {
    initialValue: { reservations: [], services: [] } as LoadedData
  });

  public searchText = signal('');
  public selectedStatus = signal<string>('');
  public selectedEmployee = signal<string>('');
  public showCancelled = signal(false);

  public employees = computed(() => {
    const data = this.loadedData();
    const employeeMap = new Map<string, string>();

    data.reservations.forEach((r) => {
      const key = `${r.employeeFirstName} ${r.employeeLastName}`;
      if (!employeeMap.has(key)) {
        employeeMap.set(key, key);
      }
    });

    return Array.from(employeeMap.values()).sort();
  });

  public filteredReservations = computed(() => {
    const data = this.loadedData();
    let result = data.reservations;
    const search = this.searchText().toLowerCase();
    const status = this.selectedStatus();
    const employee = this.selectedEmployee();
    const showCancelled = this.showCancelled();

    if (search) {
      result = result.filter((r) =>
        `${r.clientFirstName} ${r.clientLastName}`.toLowerCase().includes(search) ||
        `${r.employeeFirstName} ${r.employeeLastName}`.toLowerCase().includes(search)
      );
    }

    if (status) {
      result = result.filter((r) => r.status === status);
    }

    if (employee) {
      result = result.filter((r) =>
        `${r.employeeFirstName} ${r.employeeLastName}` === employee
      );
    }

    if (!showCancelled) {
      result = result.filter((r) => r.status !== 'CANCELLED');
    }

    return result;
  });

  public reservationsWithServices = computed<ReservationWithServices[]>(() => {
    const data = this.loadedData();
    const reservations = this.filteredReservations();
    const serviceMap = new Map<number, OfferDto>(
      data.services.map((s) => [s.id, s])
    );

    return reservations
      .map((r) => ({
        ...r,
        serviceDetails: r.serviceIds
          .map((id) => serviceMap.get(id))
          .filter((s): s is OfferDto => !!s)
      }))
      .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
  });

  public activeFiltersCount = computed(() => {
    let count = 0;
    if (this.searchText()) count++;
    if (this.selectedStatus()) count++;
    if (this.selectedEmployee()) count++;
    if (this.showCancelled()) count++;

    return count;
  });

  public setView(view: 'calendar' | 'list'): void {
    this.activeView.set(view);
  }

  public refresh(): void {
    this.refreshTrigger.set(undefined);
  }

  public clearFilters(): void {
    this.searchText.set('');
    this.selectedStatus.set('');
    this.selectedEmployee.set('');
    this.showCancelled.set(false);
  }

  public onApprove(reservation: ReservationDetailDto): void {
    const dialogRef = this.dialog.open<ConfirmationDialogComponent, ConfirmationDialogData, boolean>(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Zatwierdzenie rezerwacji',
        message: `Czy chcesz zatwierdzić rezerwację klienta ${reservation.clientFirstName} ${reservation.clientLastName}?`,
        confirmText: 'Zatwierdź',
        cancelText: 'Anuluj',
        type: 'info'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      this.reservationsService.approveReservation(reservation.reservationId).subscribe({
        next: () => {
          this.notificationService.success('Rezerwacja zatwierdzona');
          this.refresh();
        },
        error: () => this.notificationService.error('Nie udało się zatwierdzić rezerwacji')
      });
    });
  }

  public onCancel(reservation: ReservationDetailDto): void {
    const dialogRef = this.dialog.open<ConfirmationDialogComponent, ConfirmationDialogData, boolean>(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Anulowanie rezerwacji',
        message: 'Czy na pewno chcesz anulować tę rezerwację? Tej operacji nie można cofnąć.',
        confirmText: 'Anuluj rezerwację',
        cancelText: 'Wróć',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      this.reservationsService.cancelReservation(reservation.reservationId).subscribe({
        next: () => {
          this.notificationService.success('Rezerwacja anulowana');
          this.refresh();
        },
        error: () => this.notificationService.error('Nie udało się anulować rezerwacji')
      });
    });
  }
}
