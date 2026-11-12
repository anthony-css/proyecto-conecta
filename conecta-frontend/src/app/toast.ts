import { Component, effect, input, output } from '@angular/core';
import { LucideCircleCheck, LucideX } from '@lucide/angular';

@Component({
  selector: 'app-toast',
  imports: [LucideCircleCheck, LucideX],
  template: `
    @if (mensaje(); as texto) {
      <div class="feedback-toast">
        <svg lucideCircleCheck aria-hidden="true"></svg>
        <span role="status">{{ texto }}</span>
        <button class="secondary icon-button" type="button" aria-label="Cerrar aviso" (click)="cerrar.emit()">
          <svg lucideX aria-hidden="true"></svg>
        </button>
      </div>
    }
  `,
})
export class Toast {
  readonly mensaje = input.required<string>();
  readonly cerrar = output<void>();

  constructor() {
    effect((limpiar) => {
      if (!this.mensaje()) return;
      const temporizador = setTimeout(() => this.cerrar.emit(), 6000);
      limpiar(() => clearTimeout(temporizador));
    });
  }
}
