import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from './confirmation-dialog.component';
import { ConfirmationDialogData } from './confirmation-dialog.interface';
import { map, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DialogService {
  private readonly dialog = inject(MatDialog);

  public confirm(data: ConfirmationDialogData): Observable<boolean> {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data,
      disableClose: true
    });

    return dialogRef.afterClosed().pipe(
      map((result) => !!result)
    );
  }
}
