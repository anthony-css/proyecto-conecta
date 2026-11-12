import { LucideChartNoAxesColumnIncreasing, LucideChevronRight, LucideDownload, LucideUsers } from '@lucide/angular';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, debounceTime, EMPTY, finalize, from, map, startWith, Subject, switchMap } from 'rxjs';
import { environment } from '../environments/environment';
import { CatalogoApi, errorApi, estadosEvento, Evento, PaginaEventos } from './catalogo.api';
import { descargarArchivo } from './descargas';
import { exportarInscritosCsv } from './inscritos-csv';
import { AsistenciaApi, EstadoInscripcion, Inscripcion } from './asistencia.api';
import { Paginacion } from './paginacion';
import { Toast } from './toast';

const categoriasResumen: { estado: EstadoInscripcion; etiqueta: string; color: string }[] = [
  { estado: 'ASISTIO', etiqueta: 'Asistieron', color: '#28614a' },
  { estado: 'CONFIRMADA', etiqueta: 'Pendientes', color: '#c68b25' },
  { estado: 'NO_ASISTIO', etiqueta: 'No asistieron', color: '#b74720' },
  { estado: 'CANCELADA', etiqueta: 'Canceladas', color: '#667085' },
];

function resumir(inscripciones: Inscripcion[]) {
  const total = inscripciones.length;
  const cantidades: Record<EstadoInscripcion, number> = { ASISTIO: 0, CONFIRMADA: 0, NO_ASISTIO: 0, CANCELADA: 0 };
  for (const inscripcion of inscripciones) cantidades[inscripcion.estado]++;
  let acumuladas = 0;
  const filas = categoriasResumen.map((fila) => {
    const cantidad = cantidades[fila.estado];
    const inicio = total ? acumuladas * 100 / total : 0;
    acumuladas += cantidad;
    const fin = total ? acumuladas * 100 / total : 0;
    return { ...fila, cantidad, porcentaje: Math.round(fin - inicio), tramo: `${fila.color} ${inicio}% ${fin}%` };
  });
  return {
    inscripciones,
    total,
    activas: total - cantidades.CANCELADA,
    tasaCanceladas: total ? Math.round(cantidades.CANCELADA * 100 / total) : null,
    tasaAsistencia: cantidades.ASISTIO + cantidades.NO_ASISTIO
      ? Math.round(cantidades.ASISTIO * 100 / (cantidades.ASISTIO + cantidades.NO_ASISTIO)) : null,
    filas,
    dona: total ? `conic-gradient(${filas.filter((fila) => fila.cantidad).map((fila) => fila.tramo).join(', ')})` : null,
  };
}

async function errorDescarga(error: unknown): Promise<string> {
  const fallback = 'No se pudo descargar un PDF válido. Intenta nuevamente.';
  if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
    try {
      return errorApi(new HttpErrorResponse({ error: JSON.parse(await error.error.text()) }), fallback);
    } catch {
      return fallback;
    }
  }
  return errorApi(error, fallback);
}

@Component({
  imports: [ReactiveFormsModule, RouterLink, Paginacion, Toast, LucideChartNoAxesColumnIncreasing, LucideChevronRight, LucideDownload, LucideUsers],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css',
})
export class Reportes {
  private readonly http = inject(HttpClient);
  private readonly catalogo = inject(CatalogoApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly paginas = new Subject<number>();
  private readonly consultasResumen = new Subject<number>();
  private readonly asistencia = inject(AsistenciaApi);

  protected readonly buscar = new FormControl('', { nonNullable: true });
  protected readonly pagina = signal<PaginaEventos | null>(null);
  protected readonly seleccionado = signal<Evento | null>(null);
  protected readonly cargando = signal(false);
  protected readonly descargando = signal(false);
  protected readonly cargandoResumen = signal(false);
  protected readonly errorResumen = signal('');
  protected readonly resumen = signal<ReturnType<typeof resumir> | null>(null);
  protected readonly estadoGrafico = signal<EstadoInscripcion | null>(null);
  protected readonly detalleGrafico = computed(() =>
    this.resumen()?.filas.find((fila) => fila.estado === this.estadoGrafico()));
  protected readonly inscripcionesPorDia = computed(() => {
    const evento = this.seleccionado();
    const datos = this.resumen();
    if (!evento || !datos) return [];
    const altas = new Map<string, number>();
    for (const item of datos.inscripciones) {
      const fecha = item.fecha_inscripcion.slice(0, 10);
      altas.set(fecha, (altas.get(fecha) ?? 0) + 1);
    }
    const dias = Array.from({ length: 7 }, (_, indice) => {
      const dia = new Date(`${evento.fecha}T00:00:00Z`);
      dia.setUTCDate(dia.getUTCDate() - 7 + indice);
      const fecha = dia.toISOString().slice(0, 10);
      return { fecha, cantidad: altas.get(fecha) ?? 0 };
    });
    const maximo = Math.max(1, ...dias.map((dia) => dia.cantidad));
    return dias.map((dia) => ({ ...dia, altura: dia.cantidad * 100 / maximo }));
  });
  protected readonly errorEventos = signal('');
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly estados = estadosEvento;

  constructor() {
    this.paginas.pipe(
      switchMap((page) => {
        this.cargando.set(true);
        this.errorEventos.set('');
        return this.catalogo.eventos(
          { q: this.buscar.value.trim(), estado: '', id_categoria: '', fecha: '', page },
          { size: 10, sort: ['inscritos,desc', 'fecha,desc'] },
        ).pipe(
          catchError((error: unknown) => {
            this.pagina.set(null);
            this.errorEventos.set(errorApi(error, 'No se pudieron cargar los eventos.'));
            return EMPTY;
          }),
          finalize(() => this.cargando.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((pagina) => this.pagina.set(pagina));

    this.buscar.valueChanges.pipe(
      debounceTime(250), startWith(''), takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.cargar());

    this.consultasResumen.pipe(
      switchMap((id) => {
        this.resumen.set(null);
        this.estadoGrafico.set(null);
        this.errorResumen.set('');
        this.cargandoResumen.set(true);
        return this.asistencia.inscritos(id).pipe(
          map(resumir),
          catchError((error: unknown) => {
            this.errorResumen.set(errorApi(error, 'No se pudo cargar el resumen de inscripciones.'));
            return EMPTY;
          }),
          finalize(() => this.cargandoResumen.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((resumen) => this.resumen.set(resumen));

    const id = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('id_evento'));
    if (Number.isInteger(id) && id > 0 && id <= 2147483647) {
      this.catalogo.evento(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (evento) => {
          if (this.seleccionado()) return;
          this.elegir(evento);
        },
        error: (error: unknown) => this.error.set(errorApi(error, 'No se pudo seleccionar el evento solicitado.')),
      });
    }
  }

  protected cargar(page = 0): void {
    this.paginas.next(page);
  }

  protected elegir(evento: Evento): void {
    if (this.descargando()) return;
    this.seleccionado.set(evento);
    this.error.set('');
    this.aviso.set('');
    this.cargarResumen();
  }

  protected cargarResumen(): void {
    const evento = this.seleccionado();
    if (evento && !this.descargando()) this.consultasResumen.next(evento.id_evento);
  }

  protected fecha(valor: string): string {
    return valor.split('-').reverse().join('/');
  }

  protected descargar(): void {
    const evento = this.seleccionado();
    if (!evento || this.descargando() || this.cargandoResumen() || !this.resumen()?.total) return;
    const id = evento.id_evento;
    this.descargando.set(true);
    this.error.set('');
    this.aviso.set('');

    this.http.get(`${environment.apiUrl}/admin/reportes/eventos/${id}/asistencia.pdf`, { responseType: 'blob' }).pipe(
      map((archivo) => {
        if (archivo.type !== 'application/pdf' || !archivo.size) throw new HttpErrorResponse({ error: archivo });
        return archivo;
      }),
      catchError((error: unknown) => from(errorDescarga(error)).pipe(
        switchMap((mensaje) => {
          this.error.set(mensaje);
          return EMPTY;
        }),
      )),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.descargando.set(false)),
    ).subscribe((archivo) => {
      descargarArchivo(archivo, `asistencia-evento-${id}.pdf`);
      this.aviso.set('Descarga iniciada.');
    });
  }

  protected descargarCsv(): void {
    const evento = this.seleccionado();
    const resumen = this.resumen();
    if (evento && resumen?.total && !this.cargandoResumen()) {
      exportarInscritosCsv(resumen.inscripciones, evento.id_evento);
    }
  }
}
