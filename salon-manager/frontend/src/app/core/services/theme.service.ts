import { effect, Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'salon_dark_mode';

  private readonly isDarkModeSignal = signal<boolean>(this.loadThemePreference());
  public readonly isDarkMode = this.isDarkModeSignal.asReadonly();

  private constructor() {

    effect(() => {
      const isDark = this.isDarkModeSignal();
      this.applyTheme(isDark);
      this.saveThemePreference(isDark);
    });
  }

  public toggleTheme(): void {
    this.isDarkModeSignal.update((current) => !current);
  }

  public setDarkMode(enabled: boolean): void {
    this.isDarkModeSignal.set(enabled);
  }

  private applyTheme(isDark: boolean): void {
    document.body.classList.toggle('dark-theme', isDark);
  }

  private loadThemePreference(): boolean {
    return localStorage.getItem(this.THEME_KEY) === 'true';
  }

  private saveThemePreference(isDark: boolean): void {
    localStorage.setItem(this.THEME_KEY, String(isDark));
  }
}
