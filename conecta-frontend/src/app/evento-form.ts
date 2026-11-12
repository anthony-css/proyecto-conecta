import { LucideArrowLeft } from '@lucide/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin, of } from 'rxjs';
import { CatalogoApi, Categoria, errorApi, Evento, EventoDatos } from './catalogo.api';
import { FichaEvento } from './ficha-evento';

function imagenValida(value: string): boolean {
  if (!value) return true;
  try {
    const url = new URL(value);
    return /^https?:\/\//i.test(value) && !!url.hostname && !url.username && !url.password;
  } catch {
    return false;
  }
}

function horaCompleta(hora: string): string {
  return hora.length === 5 ? hora + ':00' : hora;
}

function horarioValido(form: AbstractControl) {
  const { hora_inicio, hora_fin } = form.value;
  if (!hora_inicio || !hora_fin) return null;
  return horaCompleta(hora_fin) > horaCompleta(hora_inicio)
    ? null : { horario: true };
}

@Component({
  imports: [ReactiveFormsModule, RouterLink, LucideArrowLeft, FichaEvento],
  templateUrl: './evento-form.html',
  styleUrl: './evento-form.css',
})
export class EventoForm {
  private readonly api = inject(CatalogoApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly parametro = inject(ActivatedRoute).snapshot.paramMap.get('id');
  protected readonly id = this.parametro === null ? null : Number(this.parametro);
  protected readonly evento = signal<Evento | null>(null);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carga = signal(false);
  protected readonly listo = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly conflicto = signal(false);
  protected readonly imagenRota = signal('');
  protected readonly form = inject(FormBuilder).nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(120)]],
    descripcion: ['', Validators.required],
    id_categoria: ['', Validators.required],
    fecha: ['', Validators.required],
    hora_inicio: ['', Validators.required],
    hora_fin: ['', Validators.required],
    ubicacion: ['', [Validators.required, Validators.maxLength(150)]],
    direccion: ['', Validators.maxLength(200)],
    aforo: [20, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/), Validators.max(2147483647)]],
    destacado: false,
    imagen_url: ['', [
      Validators.maxLength(255),
      (control: AbstractControl<string>) => imagenValida(control.value.trim()) ? null : { url: true },
    ]],
  }, { validators: horarioValido });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    if (this.id !== null && (!Number.isSafeInteger(this.id) || this.id <= 0 || this.id > 2147483647)) {
      this.error.set('El identificador del evento no es válido.');
      return;
    }

    this.carga.set(true);
    this.listo.set(false);
    this.error.set('');
    forkJoin({
      categorias: this.api.categorias(),
      evento: this.id === null ? of(null) : this.api.evento(this.id),
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carga.set(false)),
    ).subscribe({
      next: ({ categorias, evento }) => {
        this.categorias.set(categorias.filter((categoria) => categoria.activo));
        this.evento.set(evento);
        if (evento) {
          this.form.reset({
            ...evento,
            id_categoria: String(evento.categoria.id_categoria),
            direccion: evento.direccion ?? '',
            imagen_url: evento.imagen_url ?? '',
          });
        }
        this.conflicto.set(false);
        this.imagenRota.set('');
        this.listo.set(true);
      },
      error: (error) => this.error.set(errorApi(error, 'No se pudo cargar el formulario.')),
    });
  }

  protected categoriaActualInactiva(id: number): boolean {
    return !this.categorias().some((categoria) => categoria.id_categoria === id);
  }

  protected lectura(): boolean {
    const estado = this.evento()?.estado;
    return estado === 'REALIZADO' || estado === 'CANCELADO' || estado === 'INACTIVO';
  }

  protected imagenMostrada(): string | null {
    const control = this.form.controls.imagen_url;
    return control.valid ? control.value.trim() || null : null;
  }

  protected guardar(): void {
    if (this.guardando() || !this.listo() || this.lectura() || this.conflicto()) return;
    const datos = this.form.getRawValue();
    const idCategoria = Number(datos.id_categoria);
    if (this.form.invalid || !datos.titulo.trim() || !datos.descripcion.trim() ||
        !datos.ubicacion.trim() || !this.categorias().some((item) => item.id_categoria === idCategoria)) {
      this.form.markAllAsTouched();
      this.error.set('Revisa los campos obligatorios, el horario, la categoría activa y la URL de imagen.');
      return;
    }

    const evento: EventoDatos = {
      ...datos,
      id_categoria: idCategoria,
      titulo: datos.titulo.trim(),
      descripcion: datos.descripcion.trim(),
      ubicacion: datos.ubicacion.trim(),
      direccion: datos.direccion.trim() || null,
      imagen_url: datos.imagen_url.trim() || null,
      hora_inicio: horaCompleta(datos.hora_inicio),
      hora_fin: horaCompleta(datos.hora_fin),
      latitud: this.evento()?.latitud ?? null,
      longitud: this.evento()?.longitud ?? null,
      version: this.evento()?.version,
    };
    this.error.set('');
    this.guardando.set(true);
    this.api.guardarEvento(evento, this.id ?? undefined).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false)),
    ).subscribe({
      next: () => void this.router.navigate(['/eventos'], {
        info: { aviso: this.id === null ? 'Borrador creado.' : 'Evento actualizado.' },
      }),
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.error?.codigo === 'VERSION_CONFLICTO') {
          this.conflicto.set(true);
          this.error.set('Otro cambio se guardó primero. Recarga la versión actual antes de volver a editar.');
        } else {
          this.error.set(errorApi(error, 'No se pudo guardar el evento.'));
        }
      },
    });
  }
}
