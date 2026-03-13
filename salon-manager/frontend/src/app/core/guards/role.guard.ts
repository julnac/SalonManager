import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, _state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[];

  if (requiredRoles.length === 0) {
    return true;
  }

  const hasRole = requiredRoles.some((role) => authService.hasRole(role));

  if (hasRole) {
    return true;
  }

  void router.navigate(['/no-permissions']);

  return false;
};
