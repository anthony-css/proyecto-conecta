import { afterNextRender, Component, computed, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideCalendarDays, LucideChartNoAxesColumnIncreasing, LucideDynamicIcon, LucideLayers,
  LucideLogOut, LucideMenu, LucideScanLine, LucideTicket, LucideUserRound, LucideX } from '@lucide/angular';
import type { LucideIcon } from '@lucide/angular';
import { filter, map } from 'rxjs';
import { adminSections } from './app.routes';
import { AuthService } from './auth.service';
import { Confirmar } from './confirmar';

@Component({
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Confirmar, LucideDynamicIcon, LucideLogOut, LucideMenu, LucideTicket, LucideUserRound, LucideX],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly menu = viewChild<ElementRef<HTMLDialogElement>>('menu');
  private readonly url = toSignal(this.router.events.pipe(
    filter((evento) => evento instanceof NavigationEnd),
    map((evento) => evento.urlAfterRedirects),
  ), { initialValue: this.router.url });

  protected readonly admin = this.auth.admin;
  protected readonly sections = adminSections.filter((section) => section.path !== 'categorias');
  protected readonly enCategorias = computed(() => this.url().split('?')[0] === '/categorias');
  protected readonly menuAbierto = signal(false);
  protected readonly confirmarSalida = signal(false);
  protected readonly icons: Record<string, LucideIcon> = {
    dashboard: LucideCalendarDays,
    eventos: LucideLayers,
    asistencia: LucideScanLine,
    reportes: LucideChartNoAxesColumnIncreasing,
  };

  constructor() {
    afterNextRender(() => {
      const escritorio = window.matchMedia('(min-width: 56rem)');
      const cerrarEnEscritorio = () => {
        if (escritorio.matches) this.menu()?.nativeElement.close();
      };
      escritorio.addEventListener('change', cerrarEnEscritorio);
      this.destroyRef.onDestroy(() => escritorio.removeEventListener('change', cerrarEnEscritorio));
    });
  }

  protected abrirMenu(menu: HTMLDialogElement): void {
    menu.showModal();
    this.menuAbierto.set(true);
  }

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login'], { replaceUrl: true });
  }
}
