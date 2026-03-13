import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Observable, tap, map } from 'rxjs';
import { 
  EmployeeDto, 
  CreateEmployeeRequest, 
  UpdateEmployeeRequest, 
  EmployeeScheduleDto, 
  CreateEmployeeScheduleRequest 
} from '../models/employee.model';
import { 
  EmployeeSpecializationDto, 
  CreateEmployeeSpecializationRequest 
} from '../models/specialization.model';

@Injectable({
  providedIn: 'root',
})
export class EmployeesService {
  private readonly api = inject(ApiService);

  private readonly employeesSignal = signal<EmployeeDto[]>([]);
  public readonly employees = this.employeesSignal.asReadonly();

  private readonly specializationsSignal = signal<EmployeeSpecializationDto[]>([]);
  public readonly specializations = this.specializationsSignal.asReadonly();

  // --- PRACOWNICY ---

  public loadEmployees(): Observable<EmployeeDto[]> {
    return this.api.get<EmployeeDto[]>('employees').pipe(
      tap((data) => this.employeesSignal.set(data))
    );
  }

  public getEmployeeById(employeeId: number): Observable<EmployeeDto> {
    return this.api.get<EmployeeDto>(`employees/${employeeId}`);
  }

  public createEmployee(employeeData: Partial<CreateEmployeeRequest>): Observable<EmployeeDto> {
    return this.api.post<EmployeeDto>('employees', employeeData).pipe(
      tap((newEmployee) => this.employeesSignal.update((list) => [...list, newEmployee]))
    );
  }

  public updateEmployee(employeeId: number, employeeData: Partial<UpdateEmployeeRequest>): Observable<EmployeeDto> {
    return this.api.put<EmployeeDto>(`employees/${employeeId}`, employeeData).pipe(
      tap((updated) => this.employeesSignal.update((list) => 
        list.map((emp) => emp.id === employeeId ? updated : emp)
      ))
    );
  }

  public deleteEmployee(employeeId: number): Observable<void> {
    return this.api.delete<void>(`employees/${employeeId}`).pipe(
      tap(() => this.employeesSignal.update((list) => list.filter((emp) => emp.id !== employeeId))),
      map(() => undefined as void)
    );
  }

  // --- GRAFIKI (Schedules) ---

  public getEmployeeSchedule(employeeId: number): Observable<EmployeeScheduleDto[]> {
    return this.api.get<EmployeeScheduleDto[]>(`employees/${employeeId}/schedule`);
  }

  public createEmployeeSchedule(employeeId: number, scheduleData: CreateEmployeeScheduleRequest[]): Observable<EmployeeScheduleDto[]> {
    return this.api.post<EmployeeScheduleDto[]>(`employees/${employeeId}/schedule`, scheduleData);
  }

  public updateEmployeeSchedule(employeeId: number, scheduleData: CreateEmployeeScheduleRequest[]): Observable<EmployeeScheduleDto[]> {
    return this.api.put<EmployeeScheduleDto[]>(`employees/${employeeId}/schedule`, scheduleData);
  }

  public deleteEmployeeSchedule(employeeId: number, dayOfWeek: string): Observable<void> {
    return this.api.delete<void>(`employees/${employeeId}/schedule/${dayOfWeek}`).pipe(
      map(() => undefined as void)
    );
  }

  // --- SPECJALIZACJE ---

  public loadSpecializations(): Observable<EmployeeSpecializationDto[]> {
    return this.api.get<EmployeeSpecializationDto[]>('employee-specializations').pipe(
      tap((data) => this.specializationsSignal.set(data))
    );
  }

  public getSpecializationById(specializationId: number): Observable<EmployeeSpecializationDto> {
    return this.api.get<EmployeeSpecializationDto>(`employee-specializations/${specializationId}`);
  }

  public createSpecialization(specializationData: Partial<CreateEmployeeSpecializationRequest>): Observable<EmployeeSpecializationDto> {
    return this.api.post<EmployeeSpecializationDto>('employee-specializations', specializationData).pipe(
      tap((newSpec) => this.specializationsSignal.update((list) => [...list, newSpec]))
    );
  }

  public deleteSpecialization(specializationId: number): Observable<void> {
    return this.api.delete<void>(`employee-specializations/${specializationId}`).pipe(
      tap(() => this.specializationsSignal.update((list) => list.filter((s) => s.id !== specializationId))),
      map(() => undefined as void)
    );
  }
}