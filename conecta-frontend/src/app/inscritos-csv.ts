import { estadosInscripcion, Inscripcion, nombreInscrito } from './asistencia.api';
import { descargarArchivo } from './descargas';

function campoCsv(valor: string): string {
  const seguro = /^[\s]*[=+\-@\t\r\n]/.test(valor) ? "'" + valor : valor;
  return '"' + seguro.replaceAll('"', '""') + '"';
}

export function fechaInscripcion(valor: string | null): string {
  if (!valor) return '—';
  const [dia, hora] = valor.split('T');
  return `${dia.split('-').reverse().join('/')}${hora ? ' ' + hora.slice(0, 5) : ''}`;
}

export function exportarInscritosCsv(filas: Inscripcion[], idEvento: number): void {
  const csv = [
    ['Código', 'Nombres y apellidos', 'Correo', 'Teléfono', 'Estado', 'Fecha de inscripción', 'Fecha de asistencia'],
    ...filas.map((item) => [
      item.codigo, nombreInscrito(item), item.email, item.telefono ?? '', estadosInscripcion[item.estado],
      fechaInscripcion(item.fecha_inscripcion), fechaInscripcion(item.fecha_asistencia),
    ]),
  ].map((fila) => fila.map(campoCsv).join(',')).join('\r\n');
  descargarArchivo(new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' }), `inscritos-evento-${idEvento}.csv`);
}
