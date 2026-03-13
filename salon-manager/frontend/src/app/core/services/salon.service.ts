import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';
import { SalonProperties } from '../models/salon.model';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SalonService {
  private readonly api = inject(ApiService);

  private readonly salonSignal = signal<SalonProperties>({} as SalonProperties);
  public readonly salon = this.salonSignal.asReadonly();

  public loadSalon(): Observable<SalonProperties> {
      return this.api.get<SalonProperties>('salon').pipe(
        tap((data) => this.salonSignal.set(data))
      );
  }

}