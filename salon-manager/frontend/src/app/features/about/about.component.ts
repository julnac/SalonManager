import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/services/api.service';
import { EmployeesService } from '../../core/services/employees.service';
import { SalonService } from '../../core/services/salon.service';
import { forkJoin, finalize } from 'rxjs';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinner],
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss'
})
export class AboutComponent implements OnInit {
  private readonly apiService = inject(ApiService);
  protected readonly employeeService = inject(EmployeesService);
  protected readonly salonService = inject(SalonService);

  public isLoading = signal(false);

  public ngOnInit(): void {
    this.isLoading.set(true);

    forkJoin({
      employees: this.employeeService.loadEmployees(),
      salon: this.salonService.loadSalon()
    })
    .pipe(
      finalize(() => this.isLoading.set(false))
    )
    .subscribe({
      error: (error) => { console.error('Błąd pobierania danych:', error);}
    });
  }

  public formatTime(time: string): string {
    return time.substring(0, 5);
  }
}
