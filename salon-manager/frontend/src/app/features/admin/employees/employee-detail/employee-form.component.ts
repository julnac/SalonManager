import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { NotificationService } from '../../../../core/services/notification.service';
import { EmployeeScheduleDto } from '../../../../core/models/employee.model';
import { EmployeeSpecializationDto } from '../../../../core/models/specialization.model';
import { OfferDto } from '../../../../core/models/offer.model';
import { EmployeeFormService } from './employee-form.service';
import {
  DayOfWeek,
  EmployeeForm,
  ScheduleForm,
  SpecializationForm,
} from './employee-form.interface';
import { DialogService } from '../../../../shared/components/confirmation-dialog/dialog.service';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.scss'
})
export class EmployeeFormComponent implements OnInit {
  
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  
  private readonly notify = inject(NotificationService);
  private readonly dialogService = inject(DialogService);
  public readonly formService = inject(EmployeeFormService);

  public readonly days: DayOfWeek[] = [
    'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
  ];

  public readonly dayLabels: Record<DayOfWeek, string> = {
    MONDAY: 'Poniedziałek',
    TUESDAY: 'Wtorek',
    WEDNESDAY: 'Środa',
    THURSDAY: 'Czwartek',
    FRIDAY: 'Piątek',
    SATURDAY: 'Sobota',
    SUNDAY: 'Niedziela'
  };

  public form: FormGroup<EmployeeForm> = new FormGroup<EmployeeForm>({
    firstName: new FormControl('', { nonNullable: true, validators: Validators.required }),
    lastName: new FormControl('', { nonNullable: true, validators: Validators.required }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    specializations: new FormArray<FormGroup<SpecializationForm>>([]),
    schedule: new FormArray<FormGroup<ScheduleForm>>([])
  });

  public get specializations(): FormArray<FormGroup<SpecializationForm>> {
    return this.form.controls.specializations;
  }
  public get schedule(): FormArray<FormGroup<ScheduleForm>> {
    return this.form.controls.schedule;
  }

  public ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id && id !== 'new') {
        this.loadEmployee(Number(id));
      } else {
        this.initNewEmployee();
      }
    });
  }

  private initNewEmployee(): void {
    this.form.reset();
    this.specializations.clear();

    this.formService.loadInitialDataForNew().subscribe(() => {
      this.initDefaultSchedule();
    });
  }

  private loadEmployee(id: number): void {
    this.formService.loadInitialDataForEmployee(id).subscribe({
      next: (data) => {
        this.form.patchValue(data.employee);
        this.fillSpecializations(data.specializations);
        this.fillSchedule(data.schedule);
      },
      error: () => this.notify.error('Błąd ładowania danych')
    });
  }

  public save(): void {
    if (this.form.invalid) return;

    const rawValue = this.form.getRawValue();
    const request = {
      employee: { firstName: rawValue.firstName, lastName: rawValue.lastName, email: rawValue.email },
      schedule: rawValue.schedule.filter((s) => s.isWorkingDay),
      specialization: rawValue.specializations
    };

    const action$ = this.formService.isNewMode() 
      ? this.formService.createEmployeeData(request)
      : this.formService.updateEmployeeData(request);

    action$.subscribe({
      next: () => {
        this.notify.success('Zapisano pomyślnie');
        void this.router.navigate(['/admin/employees']);
      },
      error: () => this.notify.error('Błąd podczas zapisu')
    });
  }

  public deleteEmployee(): void {
    this.dialogService.confirm({
      title: 'Usuwanie pracownika',
      message: 'Czy na pewno chcesz usunąć tego pracownika? Tej operacji nie można cofnąć.',
      confirmText: 'Usuń',
      type: 'danger'
    }).subscribe((confirmed) => {
      if (!confirmed) return;

      this.formService.deleteEmployee().subscribe({
        next: () => {
          this.notify.success('Pracownik został usunięty');
          void this.router.navigate(['/admin/employees']);
        },
        error: () => this.notify.error('Nie udało się usunąć pracownika')
      });
    });
  }

  private fillSpecializations(specs: EmployeeSpecializationDto[]): void {
    this.specializations.clear();
    specs.forEach((s) => this.addSpecializationGroup(s.serviceId, s.experienceYears));
  }

  private fillSchedule(data: EmployeeScheduleDto[]): void {
    this.schedule.clear();
    const map = new Map(data.map((d) => [d.dayOfWeek, d]));
    this.days.forEach((day) => {
      const existing = map.get(day);
      this.schedule.push(this.createScheduleGroup(
        day, !!existing, 
        existing ? this.formatTimeFromDto(existing.startTime) : '09:00',
        existing ? this.formatTimeFromDto(existing.endTime) : '17:00'
      ));
    });
  }

  // --- Specializations ---

  public addSpecializationGroup(serviceId?: number, experienceYears?: number): void {
    this.specializations.push(new FormGroup<SpecializationForm>({
      serviceId: new FormControl(serviceId ?? 0, { nonNullable: true }),
      experienceYears: new FormControl(experienceYears ?? 0, { nonNullable: true, validators: [Validators.min(0)] })
    }));
  }

  public removeSpecializationGroup(index: number): void {
    this.specializations.removeAt(index);
  }

  public getServiceName(serviceId: number): string {
    const services = this.formService.allServices();
    const found = services.find((s) => s.id === serviceId);

    return found ? found.name : '';
  }

  public getAvailableServices(currentIndex: number): OfferDto[] {
    const allServices = this.formService.allServices();
    const selectedIds = this.specializations.controls
      .map((ctrl, idx) => (idx !== currentIndex ? ctrl.value.serviceId : null))
      .filter((id): id is number => id !== null && id !== 0);

    return allServices.filter((service) => !selectedIds.includes(service.id));
  }
  

  // --- Schedule ---

  private createScheduleGroup(day: DayOfWeek, isWorking: boolean, startTime: string, endTime: string): FormGroup<ScheduleForm> {
    return new FormGroup<ScheduleForm>({
      dayOfWeek: new FormControl(day, { nonNullable: true }),
      isWorkingDay: new FormControl(isWorking, { nonNullable: true }),
      startTime: new FormControl(startTime, { nonNullable: true }),
      endTime: new FormControl(endTime, { nonNullable: true })
    });
  }

  private initDefaultSchedule(): void {
    this.schedule.clear();
    this.days.forEach((day) => {
      const isWorking = day !== 'SATURDAY' && day !== 'SUNDAY';
      this.schedule.push(this.createScheduleGroup(day, isWorking, '09:00', '17:00'));
    });
  }

  private formatTimeFromDto(time: { hour: number; minute: number } | string): string {
    if (typeof time === 'string') {
      return time.substring(0, 5);
    }

    return `${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}`;
  }
}
