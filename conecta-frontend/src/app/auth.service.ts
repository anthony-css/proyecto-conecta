import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { map, tap } from 'rxjs';
import { environment } from '../environments/environment';

interface AdminProfile {
  username: string;
  rol: 'ROLE_ADMIN';
  nombres: string;
  apellidos: string;
}

interface AdminSession {
  profile: AdminProfile;
  authorization: string;
}

const SESSION_KEY = 'conecta.admin.session';

function isAdmin(profile: unknown): profile is AdminProfile {
  if (!profile || typeof profile !== 'object') return false;

  const candidate = profile as Partial<AdminProfile>;
  return candidate.rol === 'ROLE_ADMIN' &&
    typeof candidate.username === 'string' &&
    typeof candidate.nombres === 'string' &&
    typeof candidate.apellidos === 'string';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal(this.restoreSession());

  readonly admin = () => this.session()?.profile ?? null;

  authorization(): string | undefined {
    return this.session()?.authorization;
  }

  login(username: string, password: string) {
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    const authorization = 'Basic ' + btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''));

    return this.http.post<unknown>(`${environment.apiUrl}/auth/login`, null, {
      headers: { Authorization: authorization },
    }).pipe(
      map((profile) => {
        if (!isAdmin(profile)) throw new Error('Se requiere una cuenta administrativa.');
        return { profile, authorization };
      }),
      tap((session) => {
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
        this.session.set(session);
      }),
    );
  }

  logout(): void {
    this.session.set(null);
    sessionStorage.removeItem(SESSION_KEY);
  }

  private restoreSession(): AdminSession | null {
    try {
      const stored: Partial<AdminSession> | null = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null');
      if (isAdmin(stored?.profile) && /^Basic [A-Za-z0-9+/]+=*$/.test(stored?.authorization ?? '')) {
        return { profile: stored.profile, authorization: stored.authorization! };
      }
    } catch {
      return null;
    }
    return null;
  }
}
