import { Component, computed, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CreateOfferRequest, OfferDto, UpdateOfferRequest } from '../../../core/models/offer.model';
import { OfferFormComponent } from './offer-form/offer-form.component';
import { OfferService } from '../../../core/services/offer.service';
import { BookingService } from '../booking.service';

@Component({
  selector: 'app-offer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule
  ],
  templateUrl: './offer.component.html',
  styleUrl: './offer.component.scss'
})
export class OfferComponent implements OnInit {
  private readonly offerService = inject(OfferService);
  private readonly authService = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  protected readonly store = inject(BookingService);

  @Output() public next = new EventEmitter<void>();

  public readonly services = signal<OfferDto[]>([]);
  public readonly searchText = signal('');
  public readonly sortBy = signal('name-asc');
  public readonly isLoading = signal(false);

  public readonly isLoggedIn = computed(() => this.authService.isAuthenticated());
  public readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));

  public readonly filteredServices = computed(() => {
    let result = this.services();
    const search = this.searchText().toLowerCase().trim();

    if (search) {
      result = result.filter((s) => s.name.toLowerCase().includes(search));
    }

    const [field, direction] = this.sortBy().split('-');

    return [...result].sort((a, b) => {
      let cmp = 0;
      if (field === 'name') cmp = a.name.localeCompare(b.name);
      else if (field === 'price') cmp = a.price - b.price;
      else if (field === 'duration') cmp = a.durationMinutes - b.durationMinutes;

      return direction === 'desc' ? -cmp : cmp;
    });
  });

  public ngOnInit(): void {
    this.loadServices();
  }

  public loadServices(): void {
    this.isLoading.set(true);
    this.offerService.loadOffers().pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (data) => this.services.set(data),
      error: () => this.notify.error('Nie można pobrać listy usług')
    });
  }

  public isSelected(service: OfferDto): boolean {
    return this.store.selectedOffers().some((s) => s.id === service.id);
  }

  public toggleService(service: OfferDto): void {
    const current = this.store.selectedOffers();
    if (this.isSelected(service)) {
      this.store.selectedOffers.set(current.filter((s) => s.id !== service.id));
    } else {
      this.store.selectedOffers.set([...current, service]);
    }
  }

  public goToBooking(): void {
    if (this.store.selectedOffers().length > 0) {
      if (this.next.observed) {
        this.next.emit();
      } else {
        void this.router.navigate(['/booking']);
      }
    }
  }

  public openAddDialog(): void {
    const dialogRef = this.dialog.open(OfferFormComponent, {
      width: '400px',
      data: { mode: 'add' }
    });

    dialogRef.afterClosed().subscribe((result: CreateOfferRequest | undefined) => {
      if (!result) return;

      this.offerService.createOffer(result).subscribe({
        next: () => {
          this.notify.success('Usługa została dodana');
          this.loadServices();
        },
        error: () => this.notify.error('Nie udało się dodać usługi')
      });
    });
  }

  public openEditDialog(service: OfferDto, event: Event): void {
    event.stopPropagation();

    const dialogRef = this.dialog.open(OfferFormComponent, {
      width: '400px',
      data: { mode: 'edit', service }
    });

    dialogRef.afterClosed().subscribe((result: UpdateOfferRequest | undefined) => {
      if (result) {
        this.offerService.updateOffer(service.id, result).subscribe({
          next: () => {
            this.notify.success('Usługa została zaktualizowana');
            this.loadServices();
          },
          error: () => this.notify.error('Nie udało się zaktualizować usługi')
        });
      }
    });
  }

  public deleteService(service: OfferDto, event: Event): void {
    event.stopPropagation();

    if (!confirm(`Czy na pewno chcesz usunąć usługę "${service.name}"?`)) {
      return;
    }

    this.offerService.deleteOffer(service.id).subscribe({
      next: () => {
        this.notify.success('Usługa została usunięta');
        this.store.selectedOffers.update((list) => list.filter((s) => s.id !== service.id));
        this.loadServices();
      },
      error: () => this.notify.error('Nie udało się usunąć usługi')
    });
  }
}
