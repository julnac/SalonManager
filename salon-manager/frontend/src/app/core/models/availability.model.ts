export interface AvailabilityResponseDto {
  searchDate: string; // "YYYY-MM-DD" 
  totalDurationMinutes: number;
  employees: EmployeeAvailabilityDto[]; 
}

export interface EmployeeAvailabilityDto {
  employeeId: number;
  firstName: string;
  lastName: string;
  email: string;
  availableSlots: string[]; // "HH:mm:ss" 
}
