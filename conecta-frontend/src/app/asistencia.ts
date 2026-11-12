import { LucideCircleAlert, LucideCircleCheck, LucideKeyboard, LucideScanLine } from '@lucide/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, EMPTY, finalize, Observable, startWith, Subject, switchMap } from 'rxjs';
import { AsistenciaApi, AsistenciaRegistrada, estadosInscripcion, Inscripcion, nombreInscrito, normalizarBusqueda } from './asistencia.api';
import { CatalogoApi, errorApi, estadosEvento, Evento, PaginaEventos } from './catalogo.api';
import { QrScanner } from './qr-scanner';
import { Confirmar } from './confirmar';
import { Paginacion } from './paginacion';

function extraerToken(texto: string): string | null {
  let token = texto.trim();
  if (token.length > 512) return null;
  if (token.startsWith('{')) {
    try {
      const datos = JSON.parse(token);
      if (datos?.v !== 1 || typeof datos.token !== 'string') return null;
      token = datos.token;
    } catch {
      return null;
    }
  }
  return /^[A-Za-z0-9-]{8,64}$/.test(token) ? token : null;
}

@Component({
  imports: [ReactiveFormsModule, RouterLink, QrScanner, Confirmar, Paginacion, LucideCircleAlert, LucideCircleCheck, LucideKeyboard, LucideScanLine],
  templateUrl: './asistencia.html',
  styleUrl: './asistencia.css',
})
export class Asistencia {
  private readonly api = inject(AsistenciaApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly scanner = viewChild(QrScanner);
  private readonly paginas = new Subject<number>();
  private readonly cargarPorEvento = new Subject<number>();
  private readonly cargarHistorial = new Subject<number>();
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly buscar = new FormControl('', { nonNullable: true });
  protected readonly busquedaInscrito = signal('');
  protected readonly observacion = new FormControl('', { nonNullable: true, validators: Validators.maxLength(255) });

  protected readonly pagina = signal<PaginaEventos | null>(null);
  protected readonly evento = signal<Evento | null>(null);
  protected readonly eligiendo = signal(true);
  protected readonly modo = signal<'qr' | 'manual'>('qr');
  protected readonly cargandoEventos = signal(false);
  protected readonly errorEventos = signal('');
  protected readonly cargandoInscritos = signal(false);
  protected readonly errorInscritos = signal('');
  private readonly inscritos = signal<Inscripcion[]>([]);
  protected readonly coincidencias = computed(() => {
    const q = normalizarBusqueda(this.busquedaInscrito().trim());
    if (!q) return [];
    const exacta = this.inscritos().find((item) => normalizarBusqueda(item.codigo) === q);
    if (exacta) return [exacta];
    return this.inscritos().filter((item) =>
      normalizarBusqueda(nombreInscrito(item)).includes(q) || normalizarBusqueda(item.codigo).includes(q));
  });
  protected readonly manual = signal<Inscripcion | null>(null);
  protected readonly validando = signal(false);
  protected readonly error = signal('');
  protected readonly resultado = signal<AsistenciaRegistrada | null>(null);
  protected readonly historial = signal<AsistenciaRegistrada[]>([]);
  protected readonly cargandoHistorial = signal(false);
  protected readonly errorHistorial = signal('');
  protected readonly correccion = signal<AsistenciaRegistrada | null>(null);
  protected readonly errorCorreccion = signal('');
  protected readonly aviso = signal('');
  protected readonly duplicada = signal(false);
  protected readonly nombre = nombreInscrito;
  protected readonly estadosInscripcion = estadosInscripcion;
  protected readonly estados = estadosEvento;
  protected readonly habilitado = computed(() => {
    const estado = this.evento()?.estado;
    return estado === 'PUBLICADO' || estado === 'LLENO';
  });

  constructor() {
    this.paginas.pipe(
      switchMap((page) => {
        this.cargandoEventos.set(true);
        this.errorEventos.set('');
        return this.api.eventos(this.buscar.value.trim(), page).pipe(
          catchError((error: unknown) => {
            this.pagina.set(null);
            this.errorEventos.set(errorApi(error, 'No se pudieron cargar los eventos.'));
            return EMPTY;
          }),
          finalize(() => this.cargandoEventos.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((pagina) => this.pagina.set(pagina));

    this.buscar.valueChanges.pipe(
      debounceTime(250), startWith(''), takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.cargarEventos());

    this.cargarPorEvento.pipe(
      switchMap((id) => {
        this.cargandoInscritos.set(true);
        this.errorInscritos.set('');
        return this.api.inscritos(id).pipe(
          catchError((error: unknown) => {
            this.errorInscritos.set(errorApi(error, 'No se pudieron cargar las inscripciones.'));
            return EMPTY;
          }),
          finalize(() => this.cargandoInscritos.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((items) => this.inscritos.set(items));

    this.cargarHistorial.pipe(
      switchMap((id) => {
        this.cargandoHistorial.set(true);
        this.errorHistorial.set('');
        return this.api.historial(id).pipe(
          catchError((error: unknown) => {
            this.errorHistorial.set(errorApi(error, 'No se pudo cargar el historial de asistencia.'));
            return EMPTY;
          }),
          finalize(() => this.cargandoHistorial.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((items) => this.historial.set([...items].reverse()));

    const id = Number(this.ruta.snapshot.queryParamMap.get('id_evento'));
    if (Number.isInteger(id) && id > 0 && id <= 2147483647) {
      this.catalogo.evento(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (evento) => { if (this.eligiendo() && !this.evento()) this.seleccionar(evento); },
        error: (error: unknown) => this.errorEventos.set(errorApi(error, 'No se pudo abrir el evento solicitado.')),
      });
    }
  }

  protected cargarEventos(page = 0): void {
    this.paginas.next(page);
  }

  protected elegirOtro(): void {
    this.scanner()?.detener();
    this.eligiendo.set(true);
    this.actualizarRuta(null);
  }

  protected seleccionar(evento: Evento): void {
    if (this.validando()) return;
    if (evento.estado !== 'PUBLICADO' && evento.estado !== 'LLENO') {
      this.errorEventos.set('Este evento no está habilitado para registrar asistencia.');
      return;
    }
    this.evento.set(evento);
    this.eligiendo.set(false);
    this.modo.set('qr');
    this.resultado.set(null);
    this.error.set('');
    this.manual.set(null);
    this.busquedaInscrito.set('');
    this.observacion.reset();
    this.inscritos.set([]);
    this.historial.set([]);
    this.aviso.set('');
    this.duplicada.set(false);
    this.cargarPorEvento.next(evento.id_evento);
    this.recargarHistorial();
    this.actualizarRuta(evento.id_evento);
  }

  private actualizarRuta(id: number | null): void {
    void this.router.navigate([], {
      relativeTo: this.ruta, queryParams: { id_evento: id }, queryParamsHandling: 'merge', replaceUrl: true,
    });
  }

  protected cambiarModo(modo: 'qr' | 'manual'): void {
    if (this.validando() || this.modo() === modo) return;
    this.scanner()?.detener();
    this.modo.set(modo);
    if (modo === 'manual') this.recargarInscritos();
  }

  protected fecha(valor: string): string {
    const [dia, hora] = valor.split('T');
    return dia.split('-').reverse().join('/') + (hora ? ' · ' + hora.slice(0, 5) : '');
  }

  protected metodo(registro: AsistenciaRegistrada): string {
    return registro.metodo === 'QR' ? 'QR' : 'Manual';
  }

  protected recargarInscritos(): void {
    const evento = this.evento();
    if (evento) this.cargarPorEvento.next(evento.id_evento);
  }

  protected recargarHistorial(): void {
    const evento = this.evento();
    if (evento) this.cargarHistorial.next(evento.id_evento);
  }

  protected validarQr(texto: string): void {
    const evento = this.evento();
    if (!evento || this.eligiendo() || this.modo() !== 'qr' || !this.habilitado() || this.validando()) return;
    this.scanner()?.detener();
    const token = extraerToken(texto);
    if (!token) {
      this.resultado.set(null);
      this.duplicada.set(false);
      this.error.set('El contenido QR no tiene un formato válido.');
      return;
    }
    this.validar(this.api.validarQr(evento.id_evento, token));
  }

  protected prepararManual(item: Inscripcion): void {
    if (!this.habilitado() || this.validando() || this.cargandoInscritos()) return;
    if (item.estado !== 'CONFIRMADA') return;
    this.resultado.set(null);
    this.error.set('');
    this.duplicada.set(false);
    this.observacion.reset();
    this.manual.set(item);
  }

  protected registrarManual(): void {
    const evento = this.evento();
    const item = this.manual();
    if (!evento || !item || !this.habilitado() || this.validando() || this.observacion.invalid) return;
    this.validar(this.api.registrarManual(item.id_inscripcion, this.observacion.value.trim() || null));
  }

  protected prepararCorreccion(registro: AsistenciaRegistrada): void {
    this.scanner()?.detener();
    this.errorCorreccion.set('');
    this.correccion.set(registro);
  }

  protected deshacer(): void {
    const evento = this.evento();
    const registro = this.correccion();
    if (!evento || !registro || this.validando()) return;
    this.validando.set(true);
    this.errorCorreccion.set('');
    this.aviso.set('');
    this.api.deshacer(evento.id_evento, registro.id_asistencia).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.validando.set(false)),
    ).subscribe({
      next: () => {
        this.correccion.set(null);
        if (this.resultado()?.id_asistencia === registro.id_asistencia) this.resultado.set(null);
        this.historial.update((items) => items.filter((item) => item.id_asistencia !== registro.id_asistencia));
        this.aviso.set(`Se deshizo la asistencia de ${registro.inscripcion.participante}.`);
        this.recargarHistorial();
        this.recargarInscritos();
      },
      error: (error: unknown) => this.errorCorreccion.set(errorApi(error, 'No se pudo deshacer la asistencia.')),
    });
  }

  private validar(peticion: Observable<AsistenciaRegistrada>): void {
    this.scanner()?.detener();
    this.validando.set(true);
    this.resultado.set(null);
    this.error.set('');
    this.aviso.set('');
    this.duplicada.set(false);
    peticion.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.validando.set(false)),
    ).subscribe({
      next: (registro) => {
        this.manual.set(null);
        this.resultado.set(registro);
        this.recargarHistorial();
        this.busquedaInscrito.set('');
        this.observacion.reset();
        this.recargarInscritos();
      },
      error: (error: unknown) => {
        this.manual.set(null);
        this.error.set(errorApi(error, 'No se pudo validar la asistencia.'));
        if (error instanceof HttpErrorResponse && error.error?.codigo === 'ASISTENCIA_DUPLICADA') {
          this.duplicada.set(true);
          this.recargarInscritos();
          this.recargarHistorial();
        }
      },
    });
  }
}
