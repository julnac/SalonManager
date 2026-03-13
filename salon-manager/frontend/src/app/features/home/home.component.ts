import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { SalonProperties } from '../../core/models/salon.model';
import { ReviewDto } from '../../core/models/review.model';
import { SalonService } from '../../core/services/salon.service';
import { OfferService } from '../../core/services/offer.service';
import { forkJoin } from 'rxjs';
import { OfferDto } from '../../core/models/offer.model';
import { ReviewService } from '../../core/services/review.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  protected readonly salonService = inject(SalonService);
  protected readonly offerService = inject(OfferService);
  protected readonly reviewService = inject(ReviewService);

  public salonInfo = signal<SalonProperties>({} as SalonProperties);
  public featuredServices = signal<OfferDto[]>([]);
  public recentReviews = signal<ReviewDto[]>([]);

  public ngOnInit(): void {
    forkJoin({
      salonInfo: this.salonService.loadSalon(),
      services: this.offerService.loadOffers(),
      reviews: this.reviewService.loadReviews()
    }).subscribe({
      next: ({ salonInfo, services, reviews }) => {
        this.salonInfo.set(salonInfo);
        this.featuredServices.set(services.slice(0, 4));
        this.recentReviews.set(reviews.slice(0, 4));
      },
      error: (error) => { console.error('Błąd pobierania danych:', error);}
    });
  }

  public formatTime(time: string | undefined | null): string {
  if (!time) {
    return '--:--';
  }

  return time.substring(0, 5);
}

  public formatDate(dateString: string): string {
    const date = new Date(dateString);

    return date.toLocaleDateString('pl-PL', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
}
