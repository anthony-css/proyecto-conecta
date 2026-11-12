import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowLeft } from '@lucide/angular';
import { estadosEvento, Evento } from './catalogo.api';

@Component({
  selector: 'app-ficha-evento',
  imports: [RouterLink, LucideArrowLeft],
  template: `
    <header class="page-header">
      <div>
        <a class="event-back" routerLink="/eventos"><svg lucideArrowLeft aria-hidden="true"></svg> Eventos</a>
        <h1>{{ evento().titulo }}</h1>
        <p>{{ evento().fecha.split('-').reverse().join('/') }} · {{ evento().hora_inicio.slice(0, 5) }} · {{ evento().ubicacion }}</p>
      </div>
      <span class="badge" [attr.data-state]="evento().estado">{{ estados[evento().estado] }}</span>
    </header>
    <nav class="ficha-tabs" [attr.aria-label]="'Ficha de ' + evento().titulo">
      <a [routerLink]="['/eventos', evento().id_evento, 'editar']" [attr.aria-current]="vista() === 'informacion' ? 'page' : null">Información</a>
      <a [routerLink]="['/eventos', evento().id_evento, 'inscritos']" [attr.aria-current]="vista() === 'inscritos' ? 'page' : null">Inscritos</a>
    </nav>
  `,
  styles: [`
    h1 { overflow-wrap: anywhere; }
    .ficha-tabs { display: flex; gap: 0.4rem; margin-bottom: 1.2rem; border-bottom: 1px solid var(--con-border); }
    .ficha-tabs a { padding: 0.65rem 0.85rem; text-decoration: none; font-weight: 500; }
    .ficha-tabs a[aria-current='page'] { border-bottom: 2px solid var(--con-primary); color: var(--con-ink); }
  `],
})
export class FichaEvento {
  readonly evento = input.required<Evento>();
  readonly vista = input.required<'informacion' | 'inscritos'>();
  protected readonly estados = estadosEvento;
}
