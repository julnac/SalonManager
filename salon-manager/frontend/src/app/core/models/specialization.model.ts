export interface EmployeeSpecializationDto {
  id: number;
  employeeId: number;
  employeeName: string;
  serviceId: number;
  serviceName: string;
  experienceYears: number;
}

export interface CreateEmployeeSpecializationRequest {
  employeeId: number;
  serviceId: number;
  experienceYears?: number; // optional, min 0
}
