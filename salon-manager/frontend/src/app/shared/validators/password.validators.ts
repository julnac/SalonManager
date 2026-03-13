import { AbstractControl, ValidationErrors } from '@angular/forms';

export class PasswordValidators {
  public static match(control: AbstractControl): ValidationErrors | null {

    const passwordControl = control.get('password');
    const confirmPasswordControl = control.get('confirmPassword');

    if (!passwordControl || !confirmPasswordControl) {
      return null;
    }

    const password = passwordControl.value as string;
    const confirmPassword = confirmPasswordControl.value as string;

    return password === confirmPassword ? null : { passwordMismatch: true };
  }
}