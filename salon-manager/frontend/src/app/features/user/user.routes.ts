import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const USER_ROUTES: Routes = [
  { 
    path: '', 
    canActivate: [authGuard], 
    children: [
        { path: 'booking', loadComponent: () => import('../booking-wizard/booking/booking.component').then((m) => m.BookingComponent),},
        { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent)},
    ]
  }
];

