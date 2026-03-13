import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ReviewDto } from '../../core/models/review.model';
import { ReviewService } from '../../core/services/review.service';
import { DialogService } from '../../shared/components/confirmation-dialog/dialog.service';
import { finalize } from 'rxjs';
import { ApiImagePipe } from '../../shared/pipes/api-image.pipe';

@Component({
  selector: 'app-reviews',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    ApiImagePipe
  ],
  templateUrl: './reviews.component.html',
  styleUrl: './reviews.component.scss'
})
export class ReviewsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  protected readonly reviewService = inject(ReviewService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(DialogService);

  public readonly isLoading = signal(false);
  public readonly currentPage = signal(1);
  public readonly pageSize = signal(10);
  public readonly pageSizeOptions = [5, 10, 25, 50] as const;
  public readonly imagePreview = signal<string | null>(null);
  private selectedImage: File | null = null;

  public isLoggedIn = computed(() => this.auth.isAuthenticated());
  public isAdmin = computed(() => this.auth.hasRole('ADMIN'));

  public reviewForm = this.fb.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
  });

  public readonly paginatedReviews = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();

    return this.reviewService.sortedReviews().slice(start, start + this.pageSize());
  });

  public readonly totalReviews = computed(() => this.reviewService.reviews().length);
  public readonly totalPages = computed(() => Math.ceil(this.totalReviews() / this.pageSize()) || 1);

  public ngOnInit(): void {
    this.reviewService.loadReviews().subscribe();
  }

  public onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) return this.notify.error('Plik zbyt duży (max 5MB)');
    
    this.selectedImage = file;
    const reader = new FileReader();
    reader.onload = () => this.imagePreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  public onSubmit(): void {
    if (this.reviewForm.invalid) return;

    const user = this.auth.currentUser();
    if (!user) return this.notify.warning('Zaloguj się');

    this.isLoading.set(true);
    const formData = new FormData();
    formData.append('content', this.reviewForm.getRawValue().content);
    formData.append('userId', user.id.toString());
    if (this.selectedImage) formData.append('image', this.selectedImage);

    this.reviewService.addReview(formData).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: () => {
        this.notify.success('Opinia dodana');
        this.reviewForm.reset();
        this.removeImage();
      }
    });
  }

  public onDelete(id: number): void {
    this.dialog.confirm({
      title: 'Usuń opinię',
      message: 'Czy na pewno chcesz usunąć tę opinię?',
      type: 'danger'
    }).subscribe((confirmed) => {
      if (confirmed) {
        this.reviewService.deleteReview(id).subscribe(() => this.notify.success('Usunięto'));
      }
    });
  }

  public canDeleteReview(review: ReviewDto): boolean {
    const currentUser = this.auth.currentUser();

    return currentUser !== null && currentUser.id === review.userId;
  }

  public formatDate(dateString: string): string {
    const date = new Date(dateString);

    return date.toLocaleDateString('pl-PL', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  public removeImage(): void {
    this.selectedImage = null;
    this.imagePreview.set(null);
  }

  public onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  public previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  public nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }
}
