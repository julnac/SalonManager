import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, of } from 'rxjs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { UsersService } from '../../../core/services/users.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserDto } from '../../../core/models/user.model';
import { ClientStatisticsDto } from '../../../core/models/statistics.model';
import { MatButtonModule } from '@angular/material/button';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ConfirmationDialogData } from '../../../shared/components/confirmation-dialog/confirmation-dialog.interface';

export interface UserWithStats extends UserDto {
  statistics: ClientStatisticsDto | null;
  loadingStats: boolean;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatButtonModule,
    MatDialogModule
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private readonly usersService = inject(UsersService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  public usersWithStats = signal<UserWithStats[]>([]);
  public isLoading = signal(false);

  public displayedColumns = ['id', 'name', 'email', 'roles', 'statistics-visits', 'statistics-average-duration', 'statistics-average-spending', 'statistics-total-spending', 'operations'];

  public ngOnInit(): void {
    this.loadData();
  }

  public loadData(): void {
    this.isLoading.set(true);

    this.usersService.loadUsers().subscribe({
      next: (users) => {
        const usersWithStats: UserWithStats[] = users.filter((u: UserDto) => u.roles.includes('USER'))
          .map((user) => ({
            ...user,
            statistics: null,
            loadingStats: true
          }));
        this.usersWithStats.set(usersWithStats);
        this.isLoading.set(false);

        this.loadAllStatistics(usersWithStats);
      },
      error: () => {
        this.notificationService.error('Nie udało się pobrać użytkowników');
        this.isLoading.set(false);
      }
    });
  }

  private loadAllStatistics(users: UserDto[]): void {
    users.forEach((user) => {
      this.usersService.loadUserStatistics(user.id).pipe(
        catchError(() => of(null)) // 404 = no statistics
      ).subscribe((stats) => this.updateUserStats(user.id, stats));
    });
  }

  private updateUserStats(userId: number, stats: ClientStatisticsDto | null): void {
    this.usersWithStats.update((current) =>
      current.map((u) =>
        u.id === userId ? { ...u, statistics: stats, loadingStats: false } : u
      )
    );
  }

  public onDeleteUser(userId: number): void {
    const dialogRef = this.dialog.open<ConfirmationDialogComponent, ConfirmationDialogData, boolean>(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Usuwanie użytkownika',
        message: 'Czy na pewno chcesz usunąć tego użytkownika? Tej operacji nie można cofnąć.',
        confirmText: 'Usuń',
        cancelText: 'Anuluj',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      this.usersService.deleteUser(userId).subscribe({
        next: () => {
          this.notificationService.success('Użytkownik został usunięty');
          this.loadData();
        },
        error: () => {
          this.notificationService.error('Nie udało się usunąć użytkownika');
        }
      });
    });
  }

  public getFullName(user: UserDto): string {
    return `${user.firstName} ${user.lastName}`;
  }

  public getRoleLabel(role: string): string {
    return role.replace('ROLE_', '');
  }
}
