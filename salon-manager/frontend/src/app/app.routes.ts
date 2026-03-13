import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      { path: 'home', loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent) },
      { path: 'about', loadComponent: () => import('./features/about/about.component').then((m) => m.AboutComponent) },
      { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent) },
      { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent) },
      { path: 'offer', loadComponent: () => import('./features/booking-wizard/offer/offer.component').then((m) => m.OfferComponent) },
      { path: 'booking', loadComponent: () => import('./features/booking-wizard/booking-wizard.component').then((m) => m.BookingWizardComponent) },
      { path: 'reviews', loadComponent: () => import('./features/reviews/reviews.component').then((m) => m.ReviewsComponent) },
      { path: 'user', loadChildren: () => import('./features/user/user.routes').then((m) => m.USER_ROUTES) },
      { path: 'admin', loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES) },
      { path: '**', loadComponent: () => import('./shared/components/page-not-found/page-not-found.component').then((m) => m.PageNotFoundComponent) }
    ]
  }
];
