import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ThemeService } from '../../core/services/theme.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  public readonly themeService = inject(ThemeService);
  public readonly authService = inject(AuthService);

  public readonly mobileMenuOpen = signal(false);

  public readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));
  public readonly userNick = computed(() => this.authService.currentUser()?.firstName);

  public toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  public toggleMobileMenu(): void {
    this.mobileMenuOpen.update((v) => !v);
  }

  public logout(): void {
    this.authService.logout();
    this.mobileMenuOpen.set(false);
  }

  public closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
