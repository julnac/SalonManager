import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const activeAccountGuard: CanActivateFn = (_route, _state) => {
  const authService = inject(AuthService);
  const notificationService = inject(NotificationService);

  const currentUser = authService.currentUser();

  if (currentUser && !currentUser.enabled) {
    notificationService.warning('Twoje konto jest zawieszone. Skontaktuj się z administratorem.');

    return false;
  }

  return true;
};
