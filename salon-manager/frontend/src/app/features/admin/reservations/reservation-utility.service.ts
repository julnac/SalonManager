import { Injectable } from '@angular/core';
import { ReservationDetailDto } from '../../../core/models/reservation.model';

@Injectable({
  providedIn: 'root',
})
export class ReservationUtilityService {
  
  public getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'status-confirmed';
      case 'PENDING': return 'status-pending';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  public getStatusLabel(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'Potwierdzone';
      case 'PENDING': return 'Oczekujące';
      case 'CANCELLED': return 'Anulowane';
      default: return '';
    }
  }

  public getStatusLabels(status: ReservationDetailDto['status']): string {
    const labels: Record<ReservationDetailDto['status'], string> = {
      CREATED: 'Utworzona',
      CONFIRMED_BY_CLIENT: 'Potwierdzona',
      APPROVED_BY_SALON: 'Zatwierdzona',
      CANCELLED: 'Anulowana'
    };

    return labels[status];
  }

  public getStatusColor(status: ReservationDetailDto['status']): string {
    const colors: Record<ReservationDetailDto['status'], string> = {
      CREATED: 'accent',
      CONFIRMED_BY_CLIENT: 'primary',
      APPROVED_BY_SALON: 'primary',
      CANCELLED: 'warn'
    };

    return colors[status];
  }

  public formatTime(dateStr: string): string {
    const date = new Date(dateStr);

    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
  }

  public formatDate(dateStr: string): string {
    const date = new Date(dateStr);

    return date.toLocaleDateString('pl-PL');
  }

  public formatDateDescriptional(dateString: string): string {
    const date = new Date(dateString);

    return date.toLocaleDateString('pl-PL', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
}
