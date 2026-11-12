import { LucideCopy, LucideDownload, LucideScanLine, LucideUserCheck, LucideUserX } from '@lucide/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { AsistenciaApi, AsistenciaRegistrada, EstadoInscripcion, estadosInscripcion, Inscripcion, nombreInscrito, normalizarBusqueda } from './asistencia.api';
import { CatalogoApi, errorApi, Evento } from './catalogo.api';
import { exportarInscritosCsv, fechaInscripcion } from './inscritos-csv';
import { Confirmar } from './confirmar';
import { FichaEvento } from './ficha-evento';

@Component({
  imports: [ReactiveFormsModule, RouterLink, Confirmar, FichaEvento, LucideCopy, LucideDownload, LucideScanLine, LucideUserCheck, LucideUserX],
  templateUrl: './inscritos.html',
  styleUrl: './inscritos.css',
})
export class Inscritos {
  private readonly catalogo = inject(CatalogoApi);
  private readonly asistencia = inject(AsistenciaApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly idEvento = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  protected readonly evento = signal<Evento | null>(null);
  private readonly inscripciones = signal<Inscripcion[]>([]);
  protected readonly carga = signal(false);
  protected readonly errorCarga = signal('');
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly resultado = signal<AsistenciaRegistrada | null>(null);
  protected readonly seleccion = signal<Inscripcion | null>(null);
  protected readonly guardando = signal(false);
  protected readonly filtroBusqueda = signal('');
  protected readonly cancelacion = signal<Inscripcion | null>(null);
  protected readonly filtroEstado = signal<EstadoInscripcion | ''>('');
  protected readonly observacion = new FormControl('', { nonNullable: true, validators: Validators.maxLength(255) });
  protected readonly estados = estadosInscripcion;
  protected readonly opciones = Object.keys(estadosInscripcion) as EstadoInscripcion[];

  protected readonly visibles = computed(() => {
    const busqueda = normalizarBusqueda(this.filtroBusqueda().trim());
    const estado = this.filtroEstado();
    return this.inscripciones().filter((item) =>
      (!estado || item.estado === estado) &&
      [this.nombre(item), item.email, item.codigo].some((valor) => normalizarBusqueda(valor).includes(busqueda)));
  });

  protected readonly totales = computed(() => {
    const items = this.inscripciones();
    const porEstado = { CONFIRMADA: 0, ASISTIO: 0, NO_ASISTIO: 0, CANCELADA: 0 };
    for (const item of items) porEstado[item.estado]++;
    return { total: items.length, porEstado };
  });

  protected readonly admiteAsistencia = computed(() => {
    const estado = this.evento()?.estado;
    return estado === 'PUBLICADO' || estado === 'LLENO';
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    if (this.carga() || this.guardando()) return;
    if (!Number.isSafeInteger(this.idEvento) || this.idEvento < 1 || this.idEvento > 2147483647) {
      this.errorCarga.set('El identificador del evento no es válido.');
      return;
    }

    this.carga.set(true);
    this.errorCarga.set('');
    forkJoin({
      evento: this.catalogo.evento(this.idEvento),
      inscritos: this.asistencia.inscritos(this.idEvento),
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carga.set(false)),
    ).subscribe({
      next: ({ evento, inscritos }) => {
        this.evento.set(evento);
        this.inscripciones.set(inscritos);
      },
      error: (error) => this.errorCarga.set(errorApi(error, 'No se pudieron cargar los inscritos del evento.')),
    });
  }

  protected cambiarFiltroEstado(valor: string): void {
    this.filtroEstado.set(this.opciones.find((estado) => estado === valor) ?? '');
  }

  protected readonly nombre = nombreInscrito;

  protected puedeCancelar(item: Inscripcion): boolean {
    const evento = this.evento();
    return item.estado === 'CONFIRMADA' && !!evento &&
      (evento.estado === 'PUBLICADO' || evento.estado === 'LLENO') &&
      Date.parse(`${evento.fecha}T${evento.hora_inicio}-05:00`) > Date.now();
  }

  protected prepararCancelacion(item: Inscripcion): void {
    if (this.guardando() || !this.puedeCancelar(item)) return;
    this.error.set('');
    this.aviso.set('');
    this.resultado.set(null);
    this.cancelacion.set(item);
  }

  protected cancelarInscripcion(): void {
    const item = this.cancelacion();
    if (!item || this.guardando()) return;
    this.error.set('');
    this.guardando.set(true);
    this.asistencia.cancelarInscripcion(this.idEvento, item.id_inscripcion).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false)),
    ).subscribe({
      next: () => {
        this.inscripciones.update((items) => items.map((inscripcion) =>
          inscripcion.id_inscripcion === item.id_inscripcion ? { ...inscripcion, estado: 'CANCELADA' } : inscripcion));
        this.cancelacion.set(null);
        this.aviso.set(`Se canceló la inscripción de ${this.nombre(item)}.`);
        this.guardando.set(false);
        this.cargar();
      },
      error: (error: unknown) => this.error.set(errorApi(error, 'No se pudo cancelar la inscripción.')),
    });
  }

  protected preparar(item: Inscripcion): void {
    if (item.estado !== 'CONFIRMADA' || !this.admiteAsistencia() || this.guardando()) return;
    this.error.set('');
    this.aviso.set('');
    this.resultado.set(null);
    this.observacion.reset();
    this.seleccion.set(item);
  }

  protected registrar(): void {
    const item = this.seleccion();
    if (!item || this.guardando() || this.carga() || this.observacion.invalid) return;

    this.error.set('');
    this.guardando.set(true);
    this.asistencia.registrarManual(item.id_inscripcion, this.observacion.value.trim() || null).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false)),
    ).subscribe({
      next: (respuesta) => {
        this.inscripciones.update((items) => items.map((inscripcion) =>
          inscripcion.id_inscripcion === item.id_inscripcion
            ? { ...inscripcion, estado: 'ASISTIO', fecha_asistencia: respuesta.fecha_hora }
            : inscripcion));
        this.seleccion.set(null);
        this.resultado.set(respuesta);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.error?.codigo === 'ASISTENCIA_DUPLICADA') {
          this.seleccion.set(null);
          this.error.set('La asistencia ya fue registrada.');
          this.guardando.set(false);
          this.cargar();
        } else {
          this.error.set(errorApi(error, 'No se pudo registrar la asistencia.'));
        }
      },
    });
  }

  protected async copiar(codigo: string): Promise<void> {
    this.aviso.set('');
    try {
      await navigator.clipboard.writeText(codigo);
      this.aviso.set(`Código ${codigo} copiado.`);
    } catch {
      this.aviso.set('No se pudo copiar. Puedes seleccionar el código en la tabla.');
    }
  }

  protected exportar(): void {
    const filas = this.visibles();
    if (filas.length) exportarInscritosCsv(filas, this.idEvento);
  }

  protected readonly fecha = fechaInscripcion;
}
