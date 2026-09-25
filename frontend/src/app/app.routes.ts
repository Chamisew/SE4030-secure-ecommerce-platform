import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/pages/auth.component').then(m => m.AuthComponent),
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./features/home/pages/home/home.component').then(m => m.HomeComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'oauth2/redirect',
    loadComponent: () =>
      import('./features/auth/pages/oauth2-redirect.component').then(m => m.OAuth2RedirectComponent),
  },
  {
    path: '',
    redirectTo: 'auth',
    pathMatch: 'full',
  },
];