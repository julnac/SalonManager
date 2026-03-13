import { Injectable, inject, signal, computed } from '@angular/core';
import { EmployeesService } from '../../../../core/services/employees.service';
import { OfferService } from '../../../../core/services/offer.service';
import { EmployeeDto, EmployeeScheduleDto, CreateEmployeeRequest, CreateEmployeeScheduleRequest, UpdateEmployeeRequest} from '../../../../core/models/employee.model';
import { OfferDto } from '../../../../core/models/offer.model';
import { CreateEmployeeSpecializationRequest, EmployeeSpecializationDto } from '../../../../core/models/specialization.model';
import { Observable, forkJoin, tap, finalize, switchMap, map, of } from 'rxjs';

interface EmployeeInitialData {
  services: OfferDto[];
  employee: EmployeeDto;
  specializations: EmployeeSpecializationDto[];
  schedule: EmployeeScheduleDto[];
}

interface EmployeeData {
    employee: EmployeeDto;
    specializations: EmployeeSpecializationDto[];
    schedule: EmployeeScheduleDto[];
}

interface CreateEmployeeDataRequest {
    employee: CreateEmployeeRequest;
    schedule: CreateEmployeeScheduleRequest[];
    specialization: Omit<CreateEmployeeSpecializationRequest, 'employeeId'>[];
}

interface UpdateEmployeeDataRequest {
    employee: UpdateEmployeeRequest;
    schedule: CreateEmployeeScheduleRequest[];
    specialization: Omit<CreateEmployeeSpecializationRequest, 'employeeId'>[];
}

@Injectable({
  providedIn: 'root',
})
export class EmployeeFormService {
    private readonly employeesApi = inject(EmployeesService);
    private readonly offerApi = inject(OfferService);
    
    public readonly isLoading = signal(false);
    public readonly isSaving = signal(false);
    public readonly currentEmployee = signal<EmployeeDto | null>(null);
    public readonly currentSchedule = signal<EmployeeScheduleDto[]>([]);
    public readonly currentSpecializations = signal<EmployeeSpecializationDto[]>([]);

    public readonly allServices = signal<OfferDto[]>([]);
    public readonly allEmployees = signal<EmployeeDto[]>([]);

    public readonly isNewMode = computed(() => !this.currentEmployee());

    public updateEmployees(): void {
        this.employeesApi.loadEmployees().subscribe({
            next: (data) => {
                this.allEmployees.set(data);
            },
            error: (err) => {
                console.error('Nie udało się odświeżyć listy pracowników', err);
            }
        });
    }

    public loadInitialDataForNew(): Observable<OfferDto[]> {
        this.isLoading.set(true);
        
        this.currentEmployee.set(null);
        this.currentSchedule.set([]);
        this.currentSpecializations.set([]);

        return this.offerApi.loadOffers().pipe(
            tap((services: OfferDto[]) => {
                this.allServices.set(services);
            }),
            finalize(() => this.isLoading.set(false))
        );
    }

    public loadInitialDataForEmployee(employeeId: number): Observable<EmployeeInitialData> {
        this.isLoading.set(true);

        return forkJoin({
            services: this.offerApi.loadOffers(),
            employee: this.employeesApi.getEmployeeById(employeeId),
            specializations: this.employeesApi.loadSpecializations(),
            schedule: this.employeesApi.getEmployeeSchedule(employeeId)
        }).pipe(
            map((data) => {
                const filteredSpecs = data.specializations.filter(
                    (s) => s.employeeId === employeeId
                );

                this.allServices.set(data.services);
                this.currentEmployee.set(data.employee);
                this.currentSchedule.set(data.schedule);
                this.currentSpecializations.set(filteredSpecs);

                return {
                    services: data.services,
                    employee: data.employee,
                    specializations: filteredSpecs,
                    schedule: data.schedule
                };
            }),
            finalize(() => this.isLoading.set(false))
        );
    }

    public createEmployeeData(formData: CreateEmployeeDataRequest): Observable<EmployeeData> {
        this.isSaving.set(true);
        
        return this.employeesApi.createEmployee(formData.employee).pipe(
            switchMap((newEmployee: EmployeeDto) => {
                const id = newEmployee.id;

                const scheduleRequests = formData.schedule.map((s) => 
                    this.employeesApi.createEmployeeSchedule(id, [s])
                );
                
                const specializationRequests = formData.specialization.map((s) => {
                    const req = { ...s, employeeId: id };

                    return this.employeesApi.createSpecialization(req);
                });

                return forkJoin({
                    employee: of(newEmployee),
                    schedule: scheduleRequests.length ? forkJoin(scheduleRequests).pipe(map((res) => res.flat())) : of([]),
                    specializations: specializationRequests.length ? forkJoin(specializationRequests) : of([])
                });
            }),
            tap((result: EmployeeData) => {
                this.currentEmployee.set(result.employee);
                this.currentSchedule.set(result.schedule);
                this.currentSpecializations.set(result.specializations);
                this.updateEmployees();
            }),
            finalize(() => this.isSaving.set(false))
        );
    }

    public updateEmployeeData(formData: UpdateEmployeeDataRequest): Observable<EmployeeData> {
        this.isSaving.set(true);
        const employeeId = this.currentEmployee()?.id;

        if (!employeeId) {
            throw new Error('Brak ID pracownika do aktualizacji');
        }

        return this.employeesApi.updateEmployee(employeeId, formData.employee).pipe(
            switchMap((updatedEmployee) => {
                const formDays = formData.schedule.map((s) => s.dayOfWeek);
                const formServiceIds = formData.specialization.map((s) => s.serviceId);

                const scheduleDeleteRequests = this.currentSchedule()
                    .filter((s) => !formDays.includes(s.dayOfWeek))
                    .map((s) => this.employeesApi.deleteEmployeeSchedule(employeeId, s.dayOfWeek));

                const scheduleUpsertRequests = formData.schedule.map((newDay) => {
                    const existingDay = this.currentSchedule().find((d) => d.dayOfWeek === newDay.dayOfWeek);

                    return existingDay
                        ? this.employeesApi.updateEmployeeSchedule(employeeId, [newDay])
                        : this.employeesApi.createEmployeeSchedule(employeeId, [newDay]);
                });

                const specDeleteRequests = this.currentSpecializations()
                    .filter((s) => !formServiceIds.includes(s.serviceId))
                    .map((s) => this.employeesApi.deleteSpecialization(s.id));

                const specUpsertRequests = formData.specialization.map((newSpec) => {
                    const existingSpec = this.currentSpecializations().find((s) => s.serviceId === newSpec.serviceId);

                    if (existingSpec) {
                        return this.employeesApi.deleteSpecialization(existingSpec.id).pipe(
                            switchMap(() => this.employeesApi.createSpecialization({ ...newSpec, employeeId }))
                        );
                    }

                    return this.employeesApi.createSpecialization({ ...newSpec, employeeId });
                });

                const allScheduleRequests = [...scheduleDeleteRequests, ...scheduleUpsertRequests];
                const allSpecRequests = [...specDeleteRequests, ...specUpsertRequests];

                return forkJoin({
                    employee: of(updatedEmployee),
                    schedule: allScheduleRequests.length
                        ? forkJoin(allScheduleRequests).pipe(map((res) => res.filter((r): r is EmployeeScheduleDto[] => Array.isArray(r)).flat()))
                        : of([]),
                    specializations: allSpecRequests.length
                        ? forkJoin(allSpecRequests).pipe(map((res) => res.filter((r): r is EmployeeSpecializationDto => r !== undefined)))
                        : of([])
                });
            }),
            tap((result: EmployeeData) => {
                this.currentEmployee.set(result.employee);
                this.currentSchedule.set(result.schedule);
                this.currentSpecializations.set(result.specializations);
                this.updateEmployees();
            }),
            finalize(() => this.isSaving.set(false))
        );
    }

    public deleteEmployee(): Observable<void> {
        const employeeId = this.currentEmployee()?.id;

        if (!employeeId) {
            throw new Error('Brak ID pracownika do aktualizacji');
        }

        this.isLoading.set(true);

        return this.employeesApi.deleteEmployee(employeeId).pipe(
            tap(() => {
                this.currentEmployee.set(null);
                this.currentSchedule.set([]);
                this.currentSpecializations.set([]);
                this.updateEmployees();
            }),
            map(() => undefined as void),
            finalize(() => {
                this.isLoading.set(false);
            })
        );
    }
}