import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { UserDto } from '../models/user.model';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiService = inject(ApiService);
  private router = inject(Router);

  private readonly userSignal = signal<UserDto | null>(null);
  public readonly currentUser = this.userSignal.asReadonly();
  public readonly isAuthenticated = computed(() => this.userSignal() !== null);
  private activeCredentials = signal<string | null>(null);

  public hasRole(role: string): boolean {
    const user = this.userSignal();
    if (!user?.roles) return false;

    return user.roles.includes(`ROLE_${role}`);
  }

  public login(email: string, password: string): Observable<UserDto> {
    return this.apiService.post<UserDto>('users/login', { email, password }).pipe(
      tap((user) => {
        const creds = btoa(`${email}:${password}`);
        this.activeCredentials.set(creds);
        this.userSignal.set(user);
      }),
      catchError((error) => {
        this.userSignal.set(null);
        throw error;
      })
    );
  }

  public logout(): void {
    this.activeCredentials.set(null);
    this.userSignal.set(null);
    void this.router.navigate(['/']);
  }

  public checkSession(): void {
    this.apiService.get<UserDto>('users/me').pipe(
      catchError(() => {
        this.userSignal.set(null);

        return throwError(() => 'Session expired');
      })
    ).subscribe((user) => this.userSignal.set(user));
  }

  public getCredentials(): string | null {
    return this.activeCredentials();
  }

}
