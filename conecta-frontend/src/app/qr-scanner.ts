import { LucideCamera, LucideScanLine } from '@lucide/angular';
import { Component, ElementRef, input, OnDestroy, output, signal, viewChild } from '@angular/core';
import type { IScannerControls } from '@zxing/browser';

@Component({
  selector: 'app-qr-scanner',
  imports: [LucideCamera, LucideScanLine],
  template: `
    <div class="scanner-stage" [class.leyendo]="activa() && !abriendo()">
      <video #video [hidden]="!activa() || abriendo()" muted playsinline aria-label="Vista de la cámara"></video>
      @if (activa() && !abriendo()) {
        <div class="scanner-target" aria-hidden="true"></div>
        <span class="scanner-live" role="status">Cámara activa</span>
      } @else {
        <div class="scanner-placeholder">
          <svg lucideScanLine aria-hidden="true"></svg>
          <strong>{{ abriendo() && activa() ? 'Abriendo cámara…' : 'Escáner listo' }}</strong>
          <span>Centra el QR del pase dentro del recuadro.</span>
        </div>
      }
    </div>
    <div class="actions scanner-actions">
      @if (activa()) {
        <button type="button" class="secondary" (click)="detener()">Detener cámara</button>
      } @else {
        <button type="button" (click)="iniciar()" [disabled]="abriendo() || bloqueado()">
          <svg lucideCamera aria-hidden="true"></svg> Activar cámara
        </button>
      }
    </div>
    @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
  `,
  styles: [`
    .scanner-stage {
      position: relative;
      display: grid;
      place-items: center;
      min-height: clamp(12rem, 34vw, 17rem);
      overflow: hidden;
      border-radius: 0.75rem;
      background: var(--con-ink);
      color: white;
    }
    .scanner-stage.leyendo { height: clamp(12rem, 42vw, 21rem); }
    video { width: 100%; height: 100%; object-fit: contain; }
    .scanner-placeholder { display: grid; justify-items: center; gap: 0.5rem; padding: 1rem; text-align: center; }
    .scanner-placeholder svg { color: var(--con-accent); width: 2.75rem; height: 2.75rem; }
    .scanner-placeholder span { color: #e1e2e5; font-size: 0.85rem; }
    .scanner-target {
      position: absolute; width: min(55%, 10rem); aspect-ratio: 1; pointer-events: none;
      border: 2px solid var(--con-accent); border-radius: 0.65rem;
      box-shadow: 0 0 0 100vmax #0003;
    }
    .scanner-live {
      position: absolute; top: 0.65rem; left: 0.65rem; padding: 0.25rem 0.45rem;
      background: var(--con-ink); border-radius: 0.35rem; font-size: 0.75rem;
    }
    .scanner-actions { margin-top: 0.8rem; }
  `],
})
export class QrScanner implements OnDestroy {
  readonly leido = output<string>();
  readonly bloqueado = input(false);
  protected readonly activa = signal(false);
  protected readonly abriendo = signal(false);
  protected readonly error = signal('');

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  private stream?: MediaStream;
  private controles?: IScannerControls;

  protected async iniciar(): Promise<void> {
    if (this.bloqueado() || this.activa() || this.abriendo()) return;
    this.error.set('');
    if (!navigator.mediaDevices?.getUserMedia) {
      this.error.set('La cámara requiere HTTPS o localhost. Puedes pegar el QR o registrar manualmente.');
      return;
    }

    this.activa.set(true);
    this.abriendo.set(true);
    try {
      const { BrowserQRCodeReader } = await import('@zxing/browser');
      if (!this.activa()) return;
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: { facingMode: { ideal: 'environment' } },
      });
      if (!this.activa()) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }
      this.stream = stream;
      const video = this.video().nativeElement;
      video.muted = true;
      video.playsInline = true;
      video.srcObject = stream;
      await video.play();
      if (!this.activa()) return;
      const lector = new BrowserQRCodeReader();
      const controles = await lector.decodeFromStream(stream, video, (resultado, _error, control) => {
        if (!resultado || !this.activa()) return;
        this.controles = control;
        this.detener();
        this.leido.emit(resultado.getText());
      });
      if (this.activa()) this.controles = controles;
      else controles.stop();
    } catch (error: unknown) {
      if (!this.activa()) return;
      this.detener();
      this.error.set(error instanceof DOMException && error.name === 'NotAllowedError'
        ? 'El navegador denegó el acceso a la cámara. Puedes pegar el QR o registrar manualmente.'
        : 'No se pudo abrir la cámara. Puedes pegar el QR o registrar manualmente.');
    } finally {
      this.abriendo.set(false);
    }
  }

  detener(): void {
    this.activa.set(false);
    this.controles?.stop();
    this.controles = undefined;
    this.stream?.getTracks().forEach((track) => track.stop());
    this.stream = undefined;
  }

  ngOnDestroy(): void {
    this.detener();
  }
}
