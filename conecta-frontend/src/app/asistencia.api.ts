import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../environments/environment';
import type { PaginaEventos } from './catalogo.api';

export const estadosInscripcion = {
  CONFIRMADA: 'Confirmada',
  ASISTIO: 'Asistió',
  NO_ASISTIO: 'No asistió',
  CANCELADA: 'Cancelada',
};

export type EstadoInscripcion = keyof typeof estadosInscripcion;

export function normalizarBusqueda(valor: string): string {
  return valor.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

export function nombreInscrito(item: Inscripcion): string {
  return `${item.nombres} ${item.apellidos}`.trim();
}

export interface Inscripcion {
  id_inscripcion: number;
  codigo: string;
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string | null;
  estado: EstadoInscripcion;
  fecha_inscripcion: string;
  fecha_asistencia: string | null;
}

export interface AsistenciaRegistrada {
  id_asistencia: number;
  mensaje: string;
  inscripcion: {
    id_inscripcion: number;
    codigo: string;
    participante: string;
    email: string;
  };
  fecha_hora: string;
  metodo: 'QR' | 'MANUAL';
}

@Injectable({ providedIn: 'root' })
export class AsistenciaApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin`;

  inscritos(idEvento: number) {
    return this.http.get<Inscripcion[]>(`${this.base}/eventos/${idEvento}/inscripciones`);
  }

  eventos(q: string, page: number) {
    return this.http.get<PaginaEventos>(`${this.base}/eventos/para-asistencia`, { params: { q, page } });
  }

  historial(idEvento: number) {
    return this.http.get<AsistenciaRegistrada[]>(`${this.base}/eventos/${idEvento}/asistencias`);
  }

  deshacer(idEvento: number, idAsistencia: number) {
    return this.http.delete<void>(`${this.base}/eventos/${idEvento}/asistencias/${idAsistencia}`);
  }

  cancelarInscripcion(idEvento: number, idInscripcion: number) {
    return this.http.delete<void>(`${this.base}/eventos/${idEvento}/inscripciones/${idInscripcion}`);
  }

  validarQr(idEvento: number, token: string) {
    return this.http.post<AsistenciaRegistrada>(`${this.base}/asistencias/validar-qr`, {
      qr_token: token,
      id_evento: idEvento,
    });
  }

  registrarManual(idInscripcion: number, observacion: string | null) {
    return this.http.post<AsistenciaRegistrada>(
      `${this.base}/inscripciones/${idInscripcion}/asistencia-manual`,
      { observacion },
    );
  }
}
