import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const ADMIN_ROUTES: Routes = [
  { 
    path: '', 
    canActivate: [authGuard], 
    children: [
      {
        path: 'employees',
        loadComponent: () => import('./employees/employees.component').then((m) => m.EmployeesComponent),
        children: [
          {
            path: ':id',
            loadComponent: () => import('./employees/employee-detail/employee-form.component').then((m) => m.EmployeeFormComponent)
          }
        ]
      },
      { path: 'reservations', loadComponent: () => import('./reservations/reservations.component').then((m) => m.ReservationsComponent) },
      { path: 'users', loadComponent: () => import('./users/users.component').then((m) => m.UsersComponent) },
    ]
  }
];