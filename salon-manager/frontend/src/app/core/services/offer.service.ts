import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Observable, tap } from 'rxjs';
import { CreateOfferRequest, OfferDto, UpdateOfferRequest } from '../models/offer.model';

@Injectable({
  providedIn: 'root',
})
export class OfferService {
    private readonly api = inject(ApiService);
  
    private readonly offerSignal = signal<OfferDto[]>([]);
    public readonly offers = this.offerSignal.asReadonly();

    public loadOffers(): Observable<OfferDto[]> {
        return this.api.get<OfferDto[]>('services').pipe(
            tap((data) => this.offerSignal.set(data))
        );
    }

    public createOffer(offerData: Partial<CreateOfferRequest>): Observable<OfferDto> {
        return this.api.post<OfferDto>('services', offerData).pipe(
            tap((newOffer) => this.offerSignal.update((offers) => [...offers, newOffer]))
        );
    }

    public deleteOffer(offerId: number): Observable<void> {
        return this.api.delete<void>(`services/${offerId}`).pipe(
            tap(() => this.offerSignal.update((offers) => offers.filter((offer) => offer.id !== offerId)))
        );
    }

    public updateOffer(offerId: number, offerData: Partial<UpdateOfferRequest>): Observable<OfferDto> {
        return this.api.put<OfferDto>(`services/${offerId}`, offerData).pipe(
            tap((updatedOffer) => this.offerSignal.update((offers) =>
                offers.map((offer) => (offer.id === offerId ? updatedOffer : offer))
            ))
        );
    }

}