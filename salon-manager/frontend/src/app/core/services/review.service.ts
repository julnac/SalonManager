import { computed, inject, Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';
import { ReviewDto } from '../models/review.model';
import { tap } from 'rxjs/internal/operators/tap';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ReviewService {
  private readonly api = inject(ApiService);
  
  private readonly reviewsSignal = signal<ReviewDto[]>([]);
  public readonly reviews = this.reviewsSignal.asReadonly();

  public readonly sortedReviews = computed(() => 
    [...this.reviewsSignal()].sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  );

  public loadReviews(): Observable<ReviewDto[]> {
    return this.api.get<ReviewDto[]>('reviews').pipe(
      tap((data) => this.reviewsSignal.set(data))
    );
  }

  public addReview(formData: FormData): Observable<ReviewDto> {
    return this.api.postMultipart<ReviewDto>('reviews', formData).pipe(
      tap((newReview) => this.reviewsSignal.update((list) => [newReview, ...list]))
    );
  }

  public deleteReview(id: number): Observable<void> {
    return this.api.delete<void>(`reviews/${id}`).pipe(
      tap(() => {
        this.reviewsSignal.update((list) => list.filter((r) => r.id !== id));
      })
    );
  }

}