import { Component, computed, input, output } from '@angular/core';
import { LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

@Component({
  selector: 'app-paginacion',
  imports: [LucideChevronLeft, LucideChevronRight],
  template: `
    <nav class="table-pagination" [attr.aria-label]="'Paginación de ' + entidad()">
      <span>{{ desde() }}–{{ hasta() }} de {{ total() }} {{ entidad() }}</span>
      <div class="flex items-center gap-1">
        <button type="button" class="secondary icon-button" aria-label="Página anterior"
          [disabled]="ocupado() || pagina() === 0" (click)="cambiar.emit(pagina() - 1)">
          <svg lucideChevronLeft aria-hidden="true"></svg>
        </button>
        <span class="px-2 sm:hidden">{{ pagina() + 1 }} de {{ paginas() }}</span>
        <div class="hidden items-center gap-1 sm:flex">
          @for (numero of vecinas(); track numero) {
            <button type="button" class="secondary icon-button"
              [class.current-page]="numero === pagina()"
              [attr.aria-current]="numero === pagina() ? 'page' : null"
              [attr.aria-label]="'Página ' + (numero + 1)"
              [disabled]="ocupado() || numero === pagina()" (click)="cambiar.emit(numero)">
              {{ numero + 1 }}
            </button>
          }
        </div>
        <button type="button" class="secondary icon-button" aria-label="Página siguiente"
          [disabled]="ocupado() || pagina() + 1 >= paginas()" (click)="cambiar.emit(pagina() + 1)">
          <svg lucideChevronRight aria-hidden="true"></svg>
        </button>
      </div>
    </nav>
  `,
  styles: [`
    .table-pagination {
      display: flex; justify-content: space-between; align-items: center;
      flex-wrap: wrap; gap: 0.75rem; padding: 0.8rem; font-size: 0.85rem;
    }
    .current-page:disabled { background: var(--con-ink); color: white; opacity: 1; }
  `],
})
export class Paginacion {
  readonly pagina = input.required<number>();
  readonly paginas = input.required<number>();
  readonly total = input.required<number>();
  readonly tamano = input.required<number>();
  readonly entidad = input.required<string>();
  readonly ocupado = input(false);
  readonly cambiar = output<number>();

  protected readonly desde = computed(() => this.total() ? this.pagina() * this.tamano() + 1 : 0);
  protected readonly hasta = computed(() => Math.min((this.pagina() + 1) * this.tamano(), this.total()));
  protected readonly vecinas = computed(() => {
    const inicio = Math.max(0, Math.min(this.pagina() - 1, this.paginas() - 3));
    return Array.from({ length: Math.min(3, this.paginas()) }, (_, i) => inicio + i);
  });
}
