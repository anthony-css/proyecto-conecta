import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { AuthService } from './auth.service';

const adminGuard: CanActivateFn = () =>
  !!inject(AuthService).admin() || inject(Router).createUrlTree(['/login']);

const guestGuard: CanActivateFn = () =>
  !inject(AuthService).admin() || inject(Router).createUrlTree(['/dashboard']);

export const adminSections = [
  { path: 'dashboard', label: 'Agenda', loadComponent: () => import('./dashboard').then((m) => m.Dashboard) },
  { path: 'eventos', label: 'Eventos', loadComponent: () => import('./eventos').then((m) => m.Eventos) },
  { path: 'categorias', label: 'Categorías', loadComponent: () => import('./categorias').then((m) => m.Categorias) },
  { path: 'asistencia', label: 'Asistencia', loadComponent: () => import('./asistencia').then((m) => m.Asistencia) },
  { path: 'reportes', label: 'Reportes', loadComponent: () => import('./reportes').then((m) => m.Reportes) },
];

export const routes: Routes = [
  {
    path: 'login',
    title: 'Acceso administrativo | CONECTA',
    canActivate: [guestGuard],
    loadComponent: () => import('./login').then((module) => module.Login),
  },
  {
    path: '',
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    loadComponent: () => import('./shell').then((module) => module.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'eventos/nuevo',
        title: 'Nuevo evento | CONECTA',
        loadComponent: () => import('./evento-form').then((module) => module.EventoForm),
      },
      {
        path: 'eventos/:id/editar',
        title: 'Detalle del evento | CONECTA',
        loadComponent: () => import('./evento-form').then((module) => module.EventoForm),
      },
      {
        path: 'eventos/:id/inscritos',
        title: 'Inscritos | CONECTA',
        loadComponent: () => import('./inscritos').then((module) => module.Inscritos),
      },
      ...adminSections.map((section) => ({
        path: section.path,
        title: `${section.label} | CONECTA`,
        loadComponent: section.loadComponent,
      })),
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
