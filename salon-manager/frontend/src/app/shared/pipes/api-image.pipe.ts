import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'apiImage',
  standalone: true
})
export class ApiImagePipe implements PipeTransform {
  private readonly apiBaseUrl = 'http://localhost:8080';

  public transform(imageUrl: string | undefined | null): string {
    if (!imageUrl) return 'assets/images/placeholder-avatar.png';
    if (imageUrl.startsWith('http')) return imageUrl;
    
    const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;

    return `${this.apiBaseUrl}${path}`;
  }
}
