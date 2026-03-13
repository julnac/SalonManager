import { AvailabilityResponseDto, EmployeeAvailabilityDto } from '../../../core/models/availability.model';

export interface BookingDay {
  date: Date;
  dateStr: string;
  dayName: string;
  dayNumber: string;
  label: string;
}

export interface DayAvailability extends BookingDay {
  availability: AvailabilityResponseDto | null;
  isLoading: boolean;
}

export interface SelectedSlot {
  date: Date;
  time: string;
  employee: EmployeeAvailabilityDto;
}