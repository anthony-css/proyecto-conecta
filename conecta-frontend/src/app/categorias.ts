import { LucideArchive, LucideArrowLeft, LucidePencil, LucidePlus } from '@lucide/angular';
import { afterNextRender, Component, DestroyRef, ElementRef, inject, Injector, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CatalogoApi, Categoria, errorApi } from './catalogo.api';
import { Confirmar } from './confirmar';
import { RouterLink } from '@angular/router';
import { Toast } from './toast';

@Component({
  imports: [ReactiveFormsModule, RouterLink, Confirmar, Toast, LucideArchive, LucideArrowLeft, LucidePencil, LucidePlus],
  templateUrl: './categorias.html',
})
export class Categorias {
  private readonly api = inject(CatalogoApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly nombreInput = viewChild<ElementRef<HTMLInputElement>>('nombreInput');

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carga = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly edicion = signal<Categoria | null>(null);
  protected readonly abierta = signal(false);
  protected readonly baja = signal<Categoria | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(60)]],
    descripcion: ['', Validators.maxLength(200)],
    activo: true,
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.carga.set(true);
    this.error.set('');
    this.api.categorias().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carga.set(false)),
    ).subscribe({
      next: (items) => this.categorias.set(items),
      error: (error) => this.error.set(errorApi(error, 'No se pudieron cargar las categorías.')),
    });
  }

  protected abrir(categoria: Categoria | null = null): void {
    this.aviso.set('');
    this.error.set('');
    this.baja.set(null);
    this.edicion.set(categoria);
    this.form.reset({
      nombre: categoria?.nombre ?? '',
      descripcion: categoria?.descripcion ?? '',
      activo: categoria?.activo ?? true,
    });
    this.abierta.set(true);
    afterNextRender(() => this.nombreInput()?.nativeElement.focus(), { injector: this.injector });
  }

  protected guardar(): void {
    if (this.ocupado()) return;
    const datos = this.form.getRawValue();
    const nombre = datos.nombre.trim();
    if (this.form.invalid || !nombre) {
      this.form.markAllAsTouched();
      this.error.set('Ingresa un nombre de hasta 60 caracteres y una descripción de hasta 200.');
      return;
    }
    const categoria = this.edicion();
    const id = categoria?.id_categoria;
    this.ocupado.set(true);
    this.error.set('');
    this.api.guardarCategoria({
      ...datos, nombre, descripcion: datos.descripcion.trim() || null,
      color: categoria?.color ?? null, icono: categoria?.icono ?? null,
    }, id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.ocupado.set(false)),
    ).subscribe({
      next: () => {
        this.abierta.set(false);
        this.aviso.set(id === undefined ? 'Categoría creada.' : 'Categoría actualizada.');
        this.cargar();
      },
      error: (error) => this.error.set(errorApi(error, 'No se pudo guardar la categoría.')),
    });
  }

  protected prepararBaja(categoria: Categoria): void {
    this.abierta.set(false);
    this.error.set('');
    this.aviso.set('');
    this.baja.set(categoria);
  }

  protected eliminar(): void {
    const categoria = this.baja();
    if (!categoria || this.ocupado()) return;
    this.baja.set(null);
    this.ocupado.set(true);
    this.api.eliminarCategoria(categoria.id_categoria).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.ocupado.set(false)),
    ).subscribe({
      next: () => {
        this.aviso.set('Baja aplicada. Las categorías con eventos se conservan como inactivas.');
        this.cargar();
      },
      error: (error) => this.error.set(errorApi(error, 'No se pudo dar de baja la categoría.')),
    });
  }
}
