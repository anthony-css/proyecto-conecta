import { LucideArchive, LucideChevronDown, LucideCircleCheck, LucideCircleX, LucidePencil, LucidePlus, LucideSend, LucideTags, LucideUsers } from '@lucide/angular';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, EMPTY, finalize, Observable, startWith, Subject, switchMap } from 'rxjs';
import { CatalogoApi, Categoria, errorApi, estadosEvento, Evento, FiltrosEventos, PaginaEventos } from './catalogo.api';
import { Confirmar } from './confirmar';
import { Toast } from './toast';
import { Paginacion } from './paginacion';

type Accion = 'publicar' | 'cancelar' | 'realizar' | 'eliminar';

@Component({
  imports: [ReactiveFormsModule, RouterLink, Confirmar, Toast, Paginacion, LucideArchive, LucideChevronDown, LucideCircleCheck, LucideCircleX, LucidePencil, LucidePlus, LucideSend, LucideTags, LucideUsers],
  templateUrl: './eventos.html',
  styleUrl: './eventos.css',
})
export class Eventos {
  private readonly api = inject(CatalogoApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly consultas = new Subject<FiltrosEventos>();

  protected readonly filtros = inject(FormBuilder).nonNullable.group({
    q: '', estado: '', id_categoria: '', fecha: '',
  });
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly pagina = signal<PaginaEventos | null>(null);
  protected readonly carga = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');
  protected readonly errorCategorias = signal('');
  protected readonly aviso = signal((inject(Router).currentNavigation()?.extras.info as { aviso?: string } | undefined)?.aviso ?? '');
  protected readonly confirmacion = signal<{ evento: Evento; accion: Accion; mensaje: string } | null>(null);
  protected readonly estados = estadosEvento;
  protected readonly estadosLista = Object.entries(estadosEvento);

  constructor() {
    this.consultas.pipe(
      switchMap((filtros) => {
        this.carga.set(true);
        this.error.set('');
        return this.api.eventos(filtros).pipe(
          catchError((error: unknown) => {
            this.pagina.set(null);
            this.error.set(errorApi(error, 'No se pudieron cargar los eventos.'));
            return EMPTY;
          }),
          finalize(() => this.carga.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((pagina) => {
      if (pagina.page > 0 && pagina.content.length === 0) {
        this.cargar(Math.max(0, pagina.total_pages - 1));
      } else {
        this.pagina.set(pagina);
      }
    });
    this.filtros.valueChanges.pipe(
      debounceTime(250), startWith(null), takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      this.confirmacion.set(null);
      this.cargar();
    });
    this.api.categorias().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: (error) => this.errorCategorias.set(errorApi(error, 'No se pudieron cargar las categorías del filtro.')),
    });
  }

  protected cargar(page = 0): void {
    const filtros = this.filtros.getRawValue();
    this.consultas.next({ ...filtros, q: filtros.q.trim(), page });
  }

  protected posicionarAcciones(boton: HTMLButtonElement, menu: HTMLElement): void {
    const rect = boton.getBoundingClientRect();
    const ancho = 192;
    const altura = 236;
    const izquierda = Math.max(8, Math.min(rect.right - ancho, window.innerWidth - ancho - 8));
    menu.style.left = izquierda + 'px';
    if (window.innerHeight - rect.bottom < altura && rect.top > altura) {
      menu.style.top = 'auto';
      menu.style.bottom = (window.innerHeight - rect.top + 4) + 'px';
    } else {
      menu.style.bottom = 'auto';
      menu.style.top = (rect.bottom + 4) + 'px';
    }
  }

  protected desdeMenu(evento: Evento, accion: Accion, menu: HTMLElement, boton: HTMLButtonElement): void {
    if (this.ocupado() || this.carga()) return;
    menu.hidePopover();
    boton.focus();
    this.preparar(evento, accion);
  }

  protected editable(evento: Evento): boolean {
    return evento.estado === 'BORRADOR' || this.publicado(evento);
  }

  protected publicado(evento: Evento): boolean {
    return evento.estado === 'PUBLICADO' || evento.estado === 'LLENO';
  }

  protected puedeCerrar(evento: Evento): boolean {
    return this.publicado(evento) && Date.parse(`${evento.fecha}T${evento.hora_fin}-05:00`) <= Date.now();
  }

  protected preparar(evento: Evento, accion: Accion): void {
    const mensajes: Record<Accion, string> = {
      publicar: 'Aparecerá en el catálogo de CONECTA.',
      cancelar: 'Dejará de aparecer en el catálogo y se cancelarán las inscripciones confirmadas.',
      realizar: 'Las inscripciones confirmadas sin asistencia quedarán como «No asistió».',
      eliminar: 'Si tiene inscripciones, quedará inactivo para conservar su historial.',
    };
    this.error.set('');
    this.aviso.set('');
    this.confirmacion.set({
      evento, accion, mensaje: `¿Confirmas ${accion === 'realizar' ? 'marcar como realizado' : accion} «${evento.titulo}»? ${mensajes[accion]}`,
    });
  }

  protected confirmar(): void {
    const cambio = this.confirmacion();
    if (!cambio || this.ocupado()) return;
    this.confirmacion.set(null);
    this.ocupado.set(true);
    const { accion, evento } = cambio;
    const peticion: Observable<Evento | void> = accion === 'eliminar' ? this.api.eliminarEvento(evento.id_evento) :
      this.api.cambiarEstado(evento.id_evento, accion);
    peticion.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.ocupado.set(false)),
    ).subscribe({
      next: () => {
        this.aviso.set({
          publicar: 'Evento publicado.',
          cancelar: 'Evento cancelado.',
          realizar: 'Evento marcado como realizado. Ya puedes consultar las ausencias en Reportes.',
          eliminar: 'Baja aplicada al evento.',
        }[accion]);
        this.cargar(this.pagina()?.page ?? 0);
      },
      error: (error) => this.error.set(errorApi(error, 'No se pudo aplicar el cambio.')),
    });
  }
}
