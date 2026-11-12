import { LucideArrowRight, LucideCalendarDays, LucideEye, LucideEyeOff, LucideTicket } from '@lucide/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from './auth.service';

@Component({
  imports: [ReactiveFormsModule, LucideArrowRight, LucideCalendarDays, LucideEye, LucideEyeOff, LucideTicket],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly loading = signal(false);
  protected readonly mostrarPassword = signal(false);
  protected readonly error = signal('');
  protected readonly expired = inject(ActivatedRoute).snapshot.queryParamMap.get('motivo') === 'sesion';

  protected alternarPassword(): void {
    this.mostrarPassword.update((visible) => !visible);
  }

  protected submit(): void {
    if (this.loading()) return;
    const { username, password } = this.form.getRawValue();
    if (this.form.invalid || !username.trim()) {
      this.form.markAllAsTouched();
      this.error.set('');
      return;
    }

    this.error.set('');
    this.loading.set(true);
    this.auth.login(username.trim(), password).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: () => void this.router.navigate(['/dashboard'], { replaceUrl: true }),
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : null;
        if (status === 401) {
          this.error.set('Usuario o contraseña incorrectos.');
        } else if (status === 403 || status === null) {
          this.error.set('No se pudo iniciar una sesión administrativa.');
        } else {
          this.error.set('No pudimos conectar con el portal. Intenta nuevamente.');
        }
      },
    });
  }
}
