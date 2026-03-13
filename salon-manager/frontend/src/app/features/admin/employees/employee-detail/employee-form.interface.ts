import { FormArray, FormControl, FormGroup } from '@angular/forms';

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface SpecializationForm {
  serviceId: FormControl<number>;
  experienceYears: FormControl<number>;
}

export interface ScheduleForm {
  dayOfWeek: FormControl<DayOfWeek>;
  isWorkingDay: FormControl<boolean>;
  startTime: FormControl<string>;
  endTime: FormControl<string>;
}

export interface EmployeeForm {
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  email: FormControl<string>;
  specializations: FormArray<FormGroup<SpecializationForm>>;
  schedule: FormArray<FormGroup<ScheduleForm>>;
}
