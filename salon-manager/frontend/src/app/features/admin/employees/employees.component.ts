import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NotificationService } from '../../../core/services/notification.service';
import { EmployeeFormService } from './employee-detail/employee-form.service';

@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './employees.component.html',
  styleUrl: './employees.component.scss',
})
export class EmployeesComponent implements OnInit {
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);
  public readonly employeeFormService = inject(EmployeeFormService);

  public isLoading = signal(false);

  public ngOnInit(): void {
    this.employeeFormService.updateEmployees();
  }

  public addEmployee(): void {
    void this.router.navigate(['/admin/employees/new']);
  }

  public isActive(employeeId: number): boolean {
    return this.router.url === `/admin/employees/${employeeId}`;
  }

  public isNewActive(): boolean {
    return this.router.url === '/admin/employees/new';
  }
}
