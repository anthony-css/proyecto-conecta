import { LucideArrowRight, LucidePlus, LucideRefreshCw, LucideScanLine } from '@lucide/angular';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { environment } from '../environments/environment';
import { CatalogoApi, errorApi, estadosEvento, Evento } from './catalogo.api';

interface Resumen {
  eventos_proximos: number;
  inscripciones_activas: number;
  asistencias: number;
}

const relojLima = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'America/Lima',
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit',
  hourCycle: 'h23',
});

@Component({
  imports: [RouterLink, LucideArrowRight, LucidePlus, LucideRefreshCw, LucideScanLine],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly catalogo = inject(CatalogoApi);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly estados = estadosEvento;
  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly proximos = signal<Evento[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly indicadores: { campo: keyof Resumen; nombre: string }[] = [
    { campo: 'eventos_proximos', nombre: 'Próximos eventos' },
    { campo: 'inscripciones_activas', nombre: 'Inscripciones activas' },
    { campo: 'asistencias', nombre: 'Asistencias registradas' },
  ];

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    if (this.cargando()) return;
    this.cargando.set(true);
    this.error.set('');

    const ahora = relojLima.format(new Date());
    forkJoin({
      resumen: this.http.get<Resumen>(`${environment.apiUrl}/admin/dashboard`),
      proximos: forkJoin([
        this.buscarProximos('PUBLICADO', ahora),
        this.buscarProximos('LLENO', ahora),
      ]).pipe(map(([publicados, llenos]) => [...publicados, ...llenos].sort((a, b) =>
        this.inicio(a).localeCompare(this.inicio(b)) || a.id_evento - b.id_evento,
      ).slice(0, 5))),
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.cargando.set(false)),
    ).subscribe({
      next: ({ resumen, proximos }) => {
        this.resumen.set(resumen);
        this.proximos.set(proximos);
      },
      error: (error: unknown) => this.error.set(errorApi(error, 'No se pudo actualizar la agenda.')),
    });
  }

  protected ocupacion(evento: Evento): number {
    return evento.aforo > 0
      ? Math.max(0, Math.min(100, Math.round(evento.inscritos * 100 / evento.aforo)))
      : 0;
  }

  private inicio(evento: Evento): string {
    const hora = evento.hora_inicio.length === 5 ? evento.hora_inicio + ':00' : evento.hora_inicio;
    return `${evento.fecha} ${hora}`;
  }

  private buscarProximos(estado: 'PUBLICADO' | 'LLENO', ahora: string, page = 0, encontrados: Evento[] = []): Observable<Evento[]> {
    return this.catalogo.eventos(
      { q: '', estado, id_categoria: '', fecha: '', page },
      { size: 100, sort: ['fecha,asc', 'hora_inicio,asc'] },
    ).pipe(switchMap((pagina) => {
      const futuros = pagina.content.filter((item) => this.inicio(item) > ahora);
      const proximos = [...encontrados, ...futuros].slice(0, 5);
      return proximos.length === 5 || pagina.page + 1 >= pagina.total_pages
        ? of(proximos)
        : this.buscarProximos(estado, ahora, page + 1, proximos);
    }));
  }
}
