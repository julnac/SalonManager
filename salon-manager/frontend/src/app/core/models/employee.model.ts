export interface EmployeeDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface EmployeeScheduleDto {
  id: number;
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  startTime: { hour: number; minute: number; second: number; nano: number };
  endTime: { hour: number; minute: number; second: number; nano: number };
  isWorkingDay: boolean;
}

export interface CreateEmployeeRequest {
  firstName: string; // required, max 50 chars
  lastName: string; // required, max 50 chars
  email: string; // required, max 100 chars, email format
}

export interface UpdateEmployeeRequest {
  firstName: string; // required, max 50 chars
  lastName: string; // required, max 50 chars
  email: string; // required, max 100 chars, email format
}

export interface CreateEmployeeScheduleRequest {
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  startTime: string; // format: "HH:mm:ss"
  endTime: string; // format: "HH:mm:ss"
}
