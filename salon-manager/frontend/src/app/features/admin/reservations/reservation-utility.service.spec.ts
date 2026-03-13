import { TestBed } from '@angular/core/testing';

import { ReservationUtilityService } from './reservation-utility.service';

describe('ReservationUtilityService', () => {
  let service: ReservationUtilityService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReservationUtilityService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
