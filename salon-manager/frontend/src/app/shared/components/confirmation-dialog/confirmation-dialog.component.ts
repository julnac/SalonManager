import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ConfirmationDialogData } from './confirmation-dialog.interface';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatDialogModule],
  templateUrl: './confirmation-dialog.component.html',
  styleUrl: './confirmation-dialog.component.scss',
})
export class ConfirmationDialogComponent {
  public readonly data: ConfirmationDialogData = inject<ConfirmationDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmationDialogComponent, boolean>);

  public readonly icon = computed(() => {
    switch (this.data.type) {
      case 'danger': return 'warning';
      case 'warning': return 'help_outline';
      default: return 'info';
    }
  });

  public confirm(): void { this.dialogRef.close(true); }
  public cancel(): void { this.dialogRef.close(false); }
}
