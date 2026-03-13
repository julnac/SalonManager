import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserRegistrationDto } from '../../../core/models/user.model';
import { finalize } from 'rxjs';
import { PasswordValidators } from '../../../shared/validators/password.validators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly apiService = inject(ApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  public readonly isLoading = signal(false);

  public readonly registerForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [
      Validators.required,
      Validators.minLength(6),
      Validators.pattern(/^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).+$/)
    ]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: [PasswordValidators.match] });

  public onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();

      return;
    }

    this.isLoading.set(true);
    
    const rawValue = this.registerForm.getRawValue();

    const registrationData: UserRegistrationDto = {
      firstName: rawValue.firstName,
      lastName: rawValue.lastName,
      email: rawValue.email,
      password: rawValue.password
    };

    this.apiService.post<void>('users/register', registrationData)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: () => {
          this.notificationService.success('Konto utworzone! Możesz się teraz zalogować.');
          void this.router.navigate(['/login']);
        },
        error: (error: { status: number }) => {
          const message = error.status === 400 
            ? 'Email jest już zajęty lub dane są nieprawidłowe' 
            : 'Błąd rejestracji. Spróbuj ponownie.';
          this.notificationService.error(message);
        }
      });
    }
}
