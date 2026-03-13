import { Injectable, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  private defaultConfig: MatSnackBarConfig = {
    duration: 5000,
    horizontalPosition: 'right',
    verticalPosition: 'top',
  };

  public success(message: string, action: string = 'OK'): void {
    this.show(message, action, {
      ...this.defaultConfig,
      panelClass: ['success-snackbar']
    });
  }

  public error(message: string, action: string = 'OK'): void {
    this.show(message, action, {
      ...this.defaultConfig,
      duration: 7000,
      panelClass: ['error-snackbar']
    });
  }

  public warning(message: string, action: string = 'OK'): void {
    this.show(message, action, {
      ...this.defaultConfig,
      panelClass: ['warning-snackbar']
    });
  }

  public info(message: string, action: string = 'OK'): void {
    this.show(message, action, {
      ...this.defaultConfig,
      panelClass: ['info-snackbar']
    });
  }

  private show(message: string, action: string, config: MatSnackBarConfig): void {
    this.snackBar.open(message, action, config);
  }
}
