import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EstadoHorarioDto, RespuestaMovimientoDto } from '../models/horario.model';

@Injectable({ providedIn: 'root' })
export class HorarioApiService {
  // Ruta relativa: en desarrollo `ng serve --proxy-config proxy.conf.json` la reenvía al backend (127.0.0.1:8080).
  private readonly baseUrl = '/api/horario';

  constructor(private readonly http: HttpClient) {}

  obtenerEstado(): Observable<EstadoHorarioDto> {
    return this.http.get<EstadoHorarioDto>(this.baseUrl);
  }

  moverSesion(sesionId: string, bloqueId: string, aulaId?: string): Observable<RespuestaMovimientoDto> {
    return this.http.post<RespuestaMovimientoDto>(`${this.baseUrl}/mover`, {
      sesionId,
      bloqueId,
      aulaId: aulaId ?? ''
    });
  }

  reiniciar(cupoMaximo = 40, severidadRb1 = 'aviso', inicioSemana = 7): Observable<EstadoHorarioDto> {
    return this.http.post<EstadoHorarioDto>(`${this.baseUrl}/reiniciar`, {
      cupoMaximo: String(cupoMaximo),
      severidadRb1,
      inicioSemana: String(inicioSemana)
    });
  }
}
