import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { UserDto } from '../models/user.model';
import { ClientStatisticsDto } from '../models/statistics.model';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UsersService {
    private readonly api = inject(ApiService);
    
    private readonly usersSignal = signal<UserDto[]>([]);
    public readonly users = this.usersSignal.asReadonly();

    public loadUsers(): Observable<UserDto[]> {
        return this.api.get<UserDto[]>('users').pipe(
            tap((data) => this.usersSignal.set(data))
        );
    }

    public deleteUser(userId: number): Observable<void> {
        return this.api.delete<void>(`users/${userId}`).pipe(
            tap(() => this.usersSignal.update((users) => users.filter((user) => user.id !== userId)))
        );
    }

    public loadUserStatistics(clientId: number): Observable<ClientStatisticsDto> {
        return this.api.get<ClientStatisticsDto>(`statistics/clients/${clientId}`);
    }
}