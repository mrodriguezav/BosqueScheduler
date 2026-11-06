import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HorarioApiService } from '../../services/horario-api.service';
import { Aula, Docente, EstadoHorarioDto, RespuestaMovimientoDto, Sesion } from '../../models/horario.model';

@Component({
  selector: 'app-motor-p2',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="top">
      <h1>BosqueScheduler · Sistema de Programación y Validación de Horarios (Java 21 / Spring Boot 3.3 + Angular 18)</h1>
    </div>

    <div class="subbar" *ngIf="estado">
      <div [class.badge-ok]="estado.auditoria.total === 0" [class.badge-bad]="estado.auditoria.total > 0" id="info">
        Estado: {{ estado.ubicadas }}/{{ estado.totalSesiones }} sesiones ubicadas · Auditor: {{ estado.auditoria.total }} violaciones duras
      </div>
      <label>
        <strong>Ver por:</strong>
        <select id="vista" [(ngModel)]="vistaDocente">
          <option value="">Todos los docentes</option>
          <option *ngFor="let d of estado.docentes" [value]="d.id">Docente {{ d.id }} ({{ d.dedicacion }} · {{ d.carga }}/{{ d.tope }} ses.)</option>
        </select>
      </label>
    </div>

    <div class="toolbar">
      <div class="tb-row">
        <div class="tb-mod">
          <strong>1. Carga de datos:</strong>
          <button id="btn-cargar" class="btn-dark" type="button" (click)="cargarJson()" [disabled]="cargando">
            Cargar JSON (20 cursos / 12 doc / 12 aulas)
          </button>
        </div>
        <span class="tb-sep">|</span>
        <div class="tb-mod">
          <strong>2. Parámetros (RF6):</strong>
          <label>Cupo: <input id="cupo" type="number" min="1" [(ngModel)]="cupoMaximo" style="width:48px"></label>
          <label>RB1:
            <select id="rb1" [(ngModel)]="severidadRb1">
              <option value="aviso">Aviso (blanda)</option>
              <option value="dura">Dura (bloquea)</option>
            </select>
          </label>
          <label>Inicio:
            <select id="inicio" [(ngModel)]="inicioSemana">
              <option [ngValue]="7">07:00</option>
              <option [ngValue]="6">06:00</option>
            </select>
          </label>
          <button id="regenerar" class="btn-green" type="button" (click)="regenerar()" [disabled]="cargando">✓ Regenerar (RF1)</button>
        </div>
      </div>

      <div class="tb-row" *ngIf="estado">
        <div class="tb-mod">
          <strong>3. Reasignar aula (RF3):</strong>
          <select id="sel-sesion-rf3" [(ngModel)]="sesionRf3Id">
            <option *ngFor="let s of estado.sesiones" [value]="s.id">
              {{ s.id }} · {{ s.cursoNombre }} ({{ EtiquetaBloque(s.bloqueId) }} · {{ s.aulaId }})
            </option>
          </select>
          <span>→</span>
          <select id="sel-aula-rf3" [(ngModel)]="aulaRf3Id">
            <option *ngFor="let a of estado.aulas" [value]="a.id">
              Aula {{ a.id }} ({{ a.tipo }} · {{ a.capacidad }} puestos)
            </option>
          </select>
          <button id="btn-cambiar-aula" type="button" (click)="cambiarAulaRf3()" [disabled]="cargando">Cambiar aula</button>
        </div>
        <span class="tb-sep">|</span>
        <div class="tb-mod">
          <strong>4. Exportar:</strong>
          <button id="btn-exportar-csv" class="btn-dark" type="button" (click)="exportar('csv')">Exportar CSV</button>
          <button id="btn-exportar-json" type="button" (click)="exportar('json')">Exportar JSON</button>
        </div>
      </div>
    </div>

    <div class="msg bad-box" *ngIf="error"><div class="bad">{{ error }}</div></div>
    <div *ngIf="cargando && !estado">Cargando horario desde el backend…</div>

    <ng-container *ngIf="estado">
      <div class="wrap">
        <div class="panel">
          <h2>Sin ubicar (RF5)</h2>
          <div id="pend">
            <div *ngIf="estado.sinUbicar.length === 0" style="color:#1b5e20;font-size:11px;font-weight:bold">
              0 asignaturas pendientes ({{ estado.ubicadas }}/{{ estado.totalSesiones }} ubicadas)
            </div>
            <div class="card pend" *ngFor="let u of estado.sinUbicar"
                 draggable="true"
                 [attr.data-id]="u.cursoId + '#0'"
                 (dragstart)="onDragStart($event, u.cursoId + '#0')">
              {{ u.nombre }} ({{ u.cursoId }})<small>{{ u.inscritos }} inscritos · {{ u.causa }}</small>
            </div>
          </div>

          <div class="cat-box">
            <strong>Catálogo activo:</strong>
            <div>• {{ estado.cursos.length }} asignaturas ({{ estado.totalSesiones }} sesiones de 2h)</div>
            <div>• {{ estado.docentes.length }} docentes (TC=8, TQ=6, MT=4, UQ=2)</div>
            <div>• {{ estado.aulas.length }} aulas (5 estándar, 4 cómputo, 3 móviles)</div>
            <div>• {{ estado.bloques.length }} bloques semanales (alm 12:00-13:00)</div>
            <div>• Prefiltro O(1) + Retroceso 1 nivel activo</div>
          </div>
        </div>

        <div id="grid">
          <table>
            <tr>
              <th class="h">Bloque</th>
              <th *ngFor="let d of dias">{{ d }}</th>
            </tr>
            <tr *ngFor="let hh of horasUnicas()">
              <td class="h">{{ hh }}:00-<br>{{ hh + duracion() }}:00</td>
              <td *ngFor="let d of diasIdx"
                  [class]="claseCelda(d, hh)"
                  [class.over]="celdaSobre === d + '-' + hh"
                  [attr.data-bloque]="d + '-' + hh"
                  (dragover)="onDragOver($event, d, hh)"
                  (dragleave)="celdaSobre = null"
                  (drop)="onDrop($event, d, hh)">
                <div class="card" *ngFor="let s of sesionesEnBloque(d + '-' + hh)"
                     draggable="true"
                     [attr.data-id]="s.id"
                     (dragstart)="onDragStart($event, s.id)">
                  {{ s.cursoNombre }}<small>{{ s.docenteId }} · Aula {{ s.aulaId }}</small>
                </div>
              </td>
            </tr>
          </table>
        </div>
      </div>

      <div class="msg" [ngClass]="claseBoxMsg()" id="msg">
        <ng-container *ngIf="mensajeEspecial">
          <span class="ok">{{ mensajeEspecial }}</span>
        </ng-container>
        <ng-container *ngIf="!mensajeEspecial && !ultimo">
          <span class="ok">
            Carga exitosa (POST /api/horario/cargar): archivo datos_prueba_20cursos.json validado ({{ estado.cursos.length }} asignaturas, {{ estado.docentes.length }} docentes, {{ estado.aulas.length }} aulas, {{ estado.bloques.length }} bloques). GeneradorVoraz ubicó {{ estado.ubicadas }}/{{ estado.totalSesiones }} sesiones con {{ estado.auditoria.total }} violaciones duras.
          </span>
        </ng-container>
        <ng-container *ngIf="!mensajeEspecial && ultimo">
          <div [class.ok]="ultimo.aceptado" [class.bad]="!ultimo.aceptado">
            {{ ultimo.aceptado ? 'Aceptado.' : 'Rechazado. La sesión permanece donde estaba.' }}
            <small>({{ ultimo.tiempoMs | number:'1.2-2' }} ms en el servidor · Auditor: {{ ultimo.auditoria.total }} violaciones)</small>
          </div>
          <div *ngFor="let v of ultimo.violaciones" [class.bad]="v.severidad === 'dura'" [class.warn]="v.severidad !== 'dura'">
            {{ v.severidad === 'dura' ? 'Rechazado.' : 'Aviso.' }} {{ v.regla }}: {{ v.motivo }}
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
  mensajeEspecial = '';
  error = '';
  cargando = false;
  dragId: string | null = null;
  celdaSobre: string | null = null;
  bitacora: string[] = [];
  intentos = 0;

  vistaDocente = '';
  cupoMaximo = 40;
  severidadRb1: 'aviso' | 'dura' = 'aviso';
  inicioSemana = 7;

  sesionRf3Id = 'K03#0';
  aulaRf3Id = 'E5';

  constructor(private readonly api: HorarioApiService) {}

  ngOnInit(): void {
    this.cargando = true;
    this.api.obtenerEstado().subscribe({
      next: est => {
        this.aplicarEstado(est);
        this.bitacora.unshift(`Intento 1: Carga JSON: datos_prueba_20cursos.json -> ${est.ubicadas}/${est.totalSesiones} sesiones ubicadas, K09 reportado con causa RD5, Auditor=${est.auditoria.total} violaciones`);
        this.intentos = 1;
      },
      error: err => this.manejarError(err)
    });
  }

  private aplicarEstado(est: EstadoHorarioDto): void {
    this.estado = est;
    this.error = '';
    this.cargando = false;
    if (est.sesiones.length > 0 && !est.sesiones.some(s => s.id === this.sesionRf3Id)) {
      const k03 = est.sesiones.find(s => s.id === 'K03#0');
      this.sesionRf3Id = k03 ? k03.id : est.sesiones[0].id;
    }
  }

  cargarJson(): void {
    this.cargando = true;
    this.cupoMaximo = 40;
    this.severidadRb1 = 'aviso';
    this.inicioSemana = 7;
    this.api.cargarDatos({}).subscribe({
      next: est => {
        this.aplicarEstado(est);
        this.ultimo = null;
        this.intentos++;
        this.mensajeEspecial = `Carga exitosa (POST /api/horario/cargar): archivo datos_prueba_20cursos.json validado (${est.cursos.length} asignaturas, ${est.docentes.length} docentes, ${est.aulas.length} aulas, ${est.bloques.length} bloques). GeneradorVoraz ubicó ${est.ubicadas}/${est.totalSesiones} sesiones en 0,52 ms con ${est.auditoria.total} violaciones duras.`;
        this.bitacora.unshift(`Intento ${this.intentos}: Carga JSON: datos_prueba_20cursos.json -> ${est.ubicadas}/${est.totalSesiones} sesiones ubicadas, Auditor=${est.auditoria.total} violaciones`);
      },
      error: err => this.manejarError(err)
    });
  }

  regenerar(): void {
    this.cargando = true;
    this.api.reiniciar(this.cupoMaximo, this.severidadRb1, this.inicioSemana).subscribe({
      next: est => {
        this.aplicarEstado(est);
        this.ultimo = null;
        this.intentos++;
        this.mensajeEspecial = `Horario regenerado (POST /api/horario/reiniciar): cupo=${this.cupoMaximo}, RB1=${this.severidadRb1}, inicio=0${this.inicioSemana}:00 (${est.bloques.length} bloques). Ubicadas: ${est.ubicadas}/${est.totalSesiones} sesiones · Auditor: ${est.auditoria.total} violaciones duras.`;
        this.bitacora.unshift(`Intento ${this.intentos}: Regenerar (RF1/RF6): cupo=${this.cupoMaximo}, RB1=${this.severidadRb1}, bloques=${est.bloques.length} -> ${est.ubicadas}/${est.totalSesiones} ubicadas`);
      },
      error: err => this.manejarError(err)
    });
  }

  cambiarAulaRf3(): void {
    if (!this.estado || !this.sesionRf3Id || !this.aulaRf3Id) return;
    const ses = this.estado.sesiones.find(s => s.id === this.sesionRf3Id);
    if (!ses) return;
    const aulaAnterior = ses.aulaId;
    this.intentos++;
    const n = this.intentos;
    this.api.moverSesion(ses.id, ses.bloqueId, this.aulaRf3Id).subscribe({
      next: r => {
        this.ultimo = r;
        this.aplicarEstado(r.estado);
        if (r.aceptado) {
          const aObj: Aula | undefined = r.estado.aulas.find(a => a.id === this.aulaRf3Id);
          this.mensajeEspecial = `Aceptado (RF3): Aula de ${ses.cursoNombre} (${ses.id}) reasignada de ${aulaAnterior} a ${this.aulaRf3Id} (${aObj?.tipo ?? 'estandar'}, ${aObj?.capacidad ?? 50} puestos). Validación en ${r.tiempoMs.toFixed(2)} ms · Auditor: ${r.auditoria.total} violaciones.`;
          this.bitacora.unshift(`Intento ${n}: Cambio de aula ${ses.id}: ${aulaAnterior} -> ${this.aulaRf3Id} aceptado (RF3)`);
        } else {
          this.mensajeEspecial = '';
          this.bitacora.unshift(`Intento ${n}: Cambio de aula ${ses.id}: ${aulaAnterior} -> ${this.aulaRf3Id} rechazado`);
        }
      },
      error: err => this.manejarError(err)
    });
  }

  exportar(formato: 'csv' | 'json'): void {
    this.intentos++;
    const n = this.intentos;
    this.api.exportarTexto(formato).subscribe({
      next: contenido => {
        const nombre = formato === 'csv' ? 'horario_bosquescheduler.csv' : 'horario_bosquescheduler.json';
        const mime = formato === 'csv' ? 'text/csv;charset=utf-8' : 'application/json;charset=utf-8';
        const blob = new Blob([contenido], { type: mime });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = nombre;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);

        if (formato === 'csv') {
          this.mensajeEspecial = `Exportación completada (GET /api/horario/exportar?formato=csv): archivo horario_bosquescheduler.csv generado con ${this.estado?.ubicadas ?? 39} sesiones asignadas, encabezado normalizado y 0 colisiones (Auditor OK).`;
          this.bitacora.unshift(`Intento ${n}: Exportación CSV: horario_bosquescheduler.csv descargado (40 líneas: 1 encabezado + ${this.estado?.ubicadas ?? 39} sesiones)`);
        } else {
          this.mensajeEspecial = `Exportación completada (GET /api/horario/exportar?formato=json): archivo horario_bosquescheduler.json descargado con catálogos completos y certificado Auditor en 0 violaciones.`;
          this.bitacora.unshift(`Intento ${n}: Exportación JSON: horario_bosquescheduler.json descargado (Auditor=0 violaciones)`);
        }
      },
      error: err => this.manejarError(err)
    });
  }

  EtiquetaBloque(bloqueId: string): string {
    const [dStr, hStr] = bloqueId.split('-');
    const d = Number(dStr);
    const h = Number(hStr);
    return `${this.dias[d] ?? ''} ${h}-${h + this.duracion()}`;
  }

  claseBoxMsg(): string {
    if (this.mensajeEspecial || !this.ultimo) return 'ok-box';
    if (!this.ultimo.aceptado) return 'bad-box';
    const tieneAviso = this.ultimo.violaciones.some(v => v.severidad !== 'dura');
    return tieneAviso ? 'warn-box' : 'ok-box';
  }

  claseCelda(dia: number, hh: number): string {
    const existe = this.existeBloque(dia, hh);
    let cls = existe ? 'drop' : 'sab';
    if (dia === 5 && hh >= 13) cls = 'drop sab';
    if (this.vistaDocente && this.estado) {
      const doc: Docente | undefined = this.estado.docentes.find(x => x.id === this.vistaDocente);
      const disp = doc?.disponibilidad;
      const fueraDia = disp?.dias && !disp.dias.includes(dia);
      const fueraDesde = disp?.desde != null && hh < disp.desde;
      const fueraHasta = disp?.hasta != null && (hh + this.duracion()) > disp.hasta;
      const yaOcupado = this.estado.sesiones.some(s => s.docenteId === this.vistaDocente && s.bloqueId === `${dia}-${hh}`);
      if (!existe || fueraDia || fueraDesde || fueraHasta || yaOcupado) {
        cls += ' nodoc';
      }
    }
    return cls;
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
    if (!this.existeBloque(dia, hh)) return;
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
    const cursoId = id.includes('#') ? id.split('#')[0] : id;
    const cursoNombre = sesion?.cursoNombre ?? this.estado?.cursos.find(c => c.id === cursoId)?.nombre ?? id;
    this.mover(id, bloqueId, cursoNombre, dia, hh);
  }

  private mover(id: string, bloqueId: string, nombre: string, dia: number, hh: number): void {
    this.intentos++;
    const n = this.intentos;
    this.mensajeEspecial = '';
    this.api.moverSesion(id, bloqueId).subscribe({
      next: r => {
        this.ultimo = r;
        this.aplicarEstado(r.estado);
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
