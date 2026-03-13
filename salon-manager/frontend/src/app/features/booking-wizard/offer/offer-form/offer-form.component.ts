import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { OfferDto, CreateOfferRequest } from '../../../../core/models/offer.model';

export interface OfferFormDialogData {
  mode: 'add' | 'edit';
  service?: OfferDto;
}

@Component({
  selector: 'app-offer-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './offer-form.component.html',
  styleUrl: './offer-form.component.scss',
})
export class OfferFormComponent {

  public readonly data = inject<OfferFormDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OfferFormComponent>);

  public form = new FormGroup({
    name: new FormControl(this.data.service?.name ?? '', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    price: new FormControl(this.data.service?.price ?? 0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0.01)]
    }),
    durationMinutes: new FormControl(this.data.service?.durationMinutes ?? 30, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1)]
    })
  });

  public submit(): void {
    if (this.form.valid) {
      const result: CreateOfferRequest = this.form.getRawValue();
      this.dialogRef.close(result);
    }
  }
}