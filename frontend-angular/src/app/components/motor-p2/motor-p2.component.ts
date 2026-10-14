import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HorarioApiService } from '../../services/horario-api.service';
import { EstadoHorarioDto, RespuestaMovimientoDto, Sesion } from '../../models/horario.model';

@Component({
  selector: 'app-motor-p2',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="top">
      <h3>Motor Real (Backend Java 21 Spring Boot + Grilla Angular DOM)</h3>
      <div class="acciones">
        <label>Cupo máx. <input id="cupo" type="number" min="1" [(ngModel)]="cupoMaximo"></label>
        <label>RB1
          <select id="rb1" [(ngModel)]="severidadRb1">
            <option value="aviso">aviso</option>
            <option value="dura">dura</option>
          </select>
        </label>
        <label>Inicio L-V <input id="inicio" type="number" min="0" max="20" [(ngModel)]="inicioSemana"></label>
        <button id="regenerar" type="button" (click)="regenerar()" [disabled]="cargando">Regenerar</button>
      </div>
    </div>

    <div class="msg" *ngIf="error"><div class="bad">{{ error }}</div></div>
    <div *ngIf="cargando && !estado">Cargando horario desde el backend…</div>

    <ng-container *ngIf="estado">
      <div id="info" class="info">
        Sesiones ubicadas: {{ estado.ubicadas }} de {{ estado.totalSesiones }}.
        Evaluaciones: {{ estado.evaluaciones }} · Retrocesos: {{ estado.retrocesos }} ·
        Auditoría:
        <span [class.ok]="estado.auditoria.total === 0" [class.bad]="estado.auditoria.total > 0">
          {{ estado.auditoria.total }} violaciones duras
        </span>
      </div>
      <div class="wrap">
        <div class="panel">
          <h2>Sin ubicar (RF5)</h2>
          <div *ngIf="estado.sinUbicar.length === 0">Todas las sesiones fueron ubicadas.</div>
          <div class="card pend fija" *ngFor="let u of estado.sinUbicar">
            {{ u.nombre }}<small>{{ u.inscritos }} inscritos · {{ u.causa }}</small>
          </div>
        </div>
        <div id="grid">
          <table>
            <tr>
              <td class="h"></td>
              <th *ngFor="let d of dias">{{ d }}</th>
            </tr>
            <tr *ngFor="let hh of horasUnicas()">
              <td class="h">{{ hh }}-{{ hh + duracion() }}</td>
              <td *ngFor="let d of diasIdx"
                  [class.drop]="existeBloque(d, hh)"
                  [class.nodoc]="!existeBloque(d, hh)"
                  [class.over]="celdaSobre === d + '-' + hh"
                  [attr.data-bloque]="d + '-' + hh"
                  (dragover)="onDragOver($event, d, hh)"
                  (dragleave)="celdaSobre = null"
                  (drop)="onDrop($event, d, hh)">
                <div class="card" *ngFor="let s of sesionesEnBloque(d + '-' + hh)"
                     draggable="true"
                     [attr.data-id]="s.id"
                     (dragstart)="onDragStart($event, s.id)">
                  {{ s.cursoNombre }}<small>{{ s.docenteId }} · {{ s.aulaId }}</small>
                </div>
              </td>
            </tr>
          </table>
        </div>
      </div>
      <div class="msg" id="msg">
        <ng-container *ngIf="!ultimo">
          <span class="ok">Arrastre una tarjeta a otro bloque: el backend valida el movimiento con las mismas reglas del generador.</span>
        </ng-container>
        <ng-container *ngIf="ultimo">
          <div [class.ok]="ultimo.aceptado" [class.bad]="!ultimo.aceptado">
            {{ ultimo.aceptado ? 'Aceptado.' : 'Rechazado. La sesión permanece donde estaba.' }}
            <small>({{ ultimo.tiempoMs | number:'1.2-2' }} ms en el servidor)</small>
          </div>
          <div *ngFor="let v of ultimo.violaciones" [class.bad]="v.severidad === 'dura'" [class.warn]="v.severidad !== 'dura'">
            {{ v.severidad === 'dura' ? 'Rechazo' : 'Aviso' }} {{ v.regla }}: {{ v.motivo }}
          </div>
        </ng-container>
      </div>
      <div class="log" id="log">
        <div *ngFor="let item of bitacora">{{ item }}</div>
      </div>
    </ng-container>
  `
})
export class MotorP2Component implements OnInit {
  readonly dias = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  readonly diasIdx = [0, 1, 2, 3, 4, 5];
  estado: EstadoHorarioDto | null = null;
  ultimo: RespuestaMovimientoDto | null = null;
  error = '';
  cargando = false;
  dragId: string | null = null;
  celdaSobre: string | null = null;
  bitacora: string[] = [];
  intentos = 0;

  cupoMaximo = 40;
  severidadRb1: 'aviso' | 'dura' = 'aviso';
  inicioSemana = 7;

  constructor(private readonly api: HorarioApiService) {}

  ngOnInit(): void {
    this.cargando = true;
    this.api.obtenerEstado().subscribe({
      next: est => { this.estado = est; this.error = ''; this.cargando = false; },
      error: err => this.manejarError(err)
    });
  }

  regenerar(): void {
    this.cargando = true;
    this.api.reiniciar(this.cupoMaximo, this.severidadRb1, this.inicioSemana).subscribe({
      next: est => {
        this.estado = est;
        this.ultimo = null;
        this.error = '';
        this.cargando = false;
        this.bitacora.unshift(`Regenerado (cupo ${this.cupoMaximo}, RB1 ${this.severidadRb1}, inicio ${this.inicioSemana}): ${est.ubicadas}/${est.totalSesiones} ubicadas`);
      },
      error: err => this.manejarError(err)
    });
  }

  nombresSinUbicar(): string {
    return this.estado ? this.estado.sinUbicar.map(x => x.nombre).join(', ') : '';
  }

  duracion(): number {
    const b = this.estado?.bloques[0];
    return b ? b.fin - b.inicio : 2;
  }

  horasUnicas(): number[] {
    if (!this.estado) return [];
    return Array.from(new Set(this.estado.bloques.map(b => b.inicio))).sort((a, b) => a - b);
  }

  existeBloque(dia: number, inicio: number): boolean {
    return !!this.estado?.bloques.some(b => b.id === `${dia}-${inicio}`);
  }

  sesionesEnBloque(bloqueId: string): Sesion[] {
    return this.estado?.sesiones.filter(s => s.bloqueId === bloqueId) ?? [];
  }

  onDragStart(ev: DragEvent, id: string): void {
    this.dragId = id;
    ev.dataTransfer?.setData('text/plain', id);
  }

  onDragOver(ev: DragEvent, dia: number, hh: number): void {
    if (!this.existeBloque(dia, hh)) return; // sin preventDefault: el navegador no permite soltar
    ev.preventDefault();
    this.celdaSobre = `${dia}-${hh}`;
  }

  onDrop(ev: DragEvent, dia: number, hh: number): void {
    ev.preventDefault();
    this.celdaSobre = null;
    const id = this.dragId ?? ev.dataTransfer?.getData('text/plain') ?? '';
    this.dragId = null;
    if (!id || !this.existeBloque(dia, hh)) return;
    const bloqueId = `${dia}-${hh}`;
    const sesion = this.estado?.sesiones.find(s => s.id === id);
    if (sesion && sesion.bloqueId === bloqueId) return;
    this.mover(id, bloqueId, sesion?.cursoNombre ?? id, dia, hh);
  }

  private mover(id: string, bloqueId: string, nombre: string, dia: number, hh: number): void {
    this.intentos++;
    const n = this.intentos;
    this.api.moverSesion(id, bloqueId).subscribe({
      next: r => {
        this.ultimo = r;
        this.estado = r.estado;
        this.error = '';
        const avisos = r.violaciones.filter(v => v.severidad !== 'dura').length;
        const txt = r.aceptado ? `aceptado${avisos ? ' con aviso' : ''}` : 'rechazado';
        this.bitacora.unshift(`Intento ${n}: ${nombre} → ${this.dias[dia]} ${hh}-${hh + this.duracion()}: ${txt}`);
      },
      error: err => this.manejarError(err)
    });
  }

  private manejarError(err: HttpErrorResponse): void {
    this.cargando = false;
    if (err.status === 0) {
      this.error = 'No hay conexión con el backend (http://127.0.0.1:8080). Inicie el servidor Java y recargue la página.';
    } else {
      const detalle = (err.error && typeof err.error === 'object' && 'error' in err.error) ? err.error.error : err.message;
      this.error = `Error ${err.status}: ${detalle}`;
    }
  }
}
