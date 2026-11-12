import { afterNextRender, Component, ElementRef, input, OnDestroy, output, viewChild } from '@angular/core';
import { LucideCircleAlert } from '@lucide/angular';

@Component({
  selector: 'app-confirmar',
  imports: [LucideCircleAlert],
  template: `
    <dialog #dialogo class="confirm-dialog" aria-labelledby="confirmar-titulo" aria-describedby="confirmar-mensaje"
      (cancel)="$event.preventDefault(); cancelar()">
      <span class="confirm-symbol"><svg lucideCircleAlert aria-hidden="true"></svg></span>
      <h2 id="confirmar-titulo">Confirmar acción</h2>
      <p id="confirmar-mensaje">{{ mensaje() }}</p>
      <ng-content />
      <div class="actions justify-end">
        <button type="button" class="secondary" autofocus [disabled]="ocupado()" (click)="cancelar()">Volver</button>
        <button type="button" [disabled]="ocupado() || invalido()" (click)="aceptar.emit()">{{ etiqueta() }}</button>
      </div>
    </dialog>
  `,
})
export class Confirmar implements OnDestroy {
  readonly mensaje = input.required<string>();
  readonly etiqueta = input('Confirmar');
  readonly ocupado = input(false);
  readonly invalido = input(false);
  readonly aceptar = output<void>();
  readonly volver = output<void>();
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private abierto?: HTMLDialogElement;
  private origen?: HTMLElement;

  constructor() {
    afterNextRender(() => {
      if (document.activeElement instanceof HTMLElement) this.origen = document.activeElement;
      this.abierto = this.dialogo().nativeElement;
      this.abierto.showModal();
    });
  }

  protected cancelar(): void {
    if (!this.ocupado()) this.volver.emit();
  }

  ngOnDestroy(): void {
    this.abierto?.close();
    if (this.origen?.isConnected) this.origen.focus();
  }
}
