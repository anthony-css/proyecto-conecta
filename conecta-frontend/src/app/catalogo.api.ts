import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../environments/environment';

export interface CategoriaDatos {
  nombre: string;
  descripcion: string | null;
  color: string | null;
  icono: string | null;
  activo: boolean;
}

export interface Categoria extends CategoriaDatos {
  id_categoria: number;
}

export const estadosEvento = {
  BORRADOR: 'Borrador',
  PUBLICADO: 'Publicado',
  LLENO: 'Completo',
  REALIZADO: 'Realizado',
  CANCELADO: 'Cancelado',
  INACTIVO: 'Inactivo',
};

export type EstadoEvento = keyof typeof estadosEvento;

interface DetalleEvento {
  titulo: string;
  descripcion: string;
  imagen_url: string | null;
  fecha: string;
  hora_inicio: string;
  hora_fin: string;
  ubicacion: string;
  direccion: string | null;
  latitud: number | null;
  longitud: number | null;
  aforo: number;
  destacado: boolean;
}

export interface Evento extends DetalleEvento {
  id_evento: number;
  categoria: Pick<Categoria, 'id_categoria' | 'nombre' | 'color' | 'icono'>;
  inscritos: number;
  cupos_disponibles: number;
  estado: EstadoEvento;
  version: number;
}

export interface EventoDatos extends DetalleEvento {
  id_categoria: number;
  version?: number;
}

export interface FiltrosEventos {
  q: string;
  estado: string;
  id_categoria: string;
  fecha: string;
  page: number;
}

export interface PaginaEventos {
  content: Evento[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

export function errorApi(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const { mensaje, detalle } = error.error ?? {};
    if (typeof mensaje === 'string') {
      return typeof detalle === 'string' && detalle ? `${mensaje}. ${detalle}` : mensaje;
    }
  }
  return fallback;
}

@Injectable({ providedIn: 'root' })
export class CatalogoApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin`;

  categorias() {
    return this.http.get<Categoria[]>(`${this.base}/categorias`);
  }

  guardarCategoria(datos: CategoriaDatos, id?: number) {
    const url = `${this.base}/categorias`;
    return id === undefined ? this.http.post<Categoria>(url, datos) :
      this.http.put<Categoria>(`${url}/${id}`, datos);
  }

  eliminarCategoria(id: number) {
    return this.http.delete<void>(`${this.base}/categorias/${id}`);
  }

  eventos(filtros: FiltrosEventos, opciones = { size: 10, sort: ['fecha,desc'] }) {
    let params = new HttpParams({ fromObject: { page: filtros.page, size: opciones.size, sort: opciones.sort } });
    for (const campo of ['q', 'estado', 'id_categoria', 'fecha'] as const) {
      if (filtros[campo]) params = params.set(campo, filtros[campo]);
    }
    return this.http.get<PaginaEventos>(`${this.base}/eventos`, { params });
  }

  evento(id: number) {
    return this.http.get<Evento>(`${this.base}/eventos/${id}`);
  }

  guardarEvento(datos: EventoDatos, id?: number) {
    const url = `${this.base}/eventos`;
    return id === undefined ? this.http.post<Evento>(url, datos) :
      this.http.put<Evento>(`${url}/${id}`, datos);
  }

  cambiarEstado(id: number, accion: 'publicar' | 'cancelar' | 'realizar') {
    return this.http.patch<Evento>(`${this.base}/eventos/${id}/${accion}`, null);
  }

  eliminarEvento(id: number) {
    return this.http.delete<void>(`${this.base}/eventos/${id}`);
  }
}
