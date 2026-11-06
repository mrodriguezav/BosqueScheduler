import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface SesionP1 {
  id: string;
  c: string;
  d: string;
  a: string;
  dia: number;
  h: number;
}

@Component({
  selector: 'app-wireframe-p1',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="top">
      <h1>BosqueScheduler · Sistema de Programación y Validación de Horarios (Java 21 / Spring Boot 3.3 + Angular 18)</h1>
    </div>
    <div class="subbar">
      <div class="badge-ok">Estado: 39/41 sesiones ubicadas · Auditor: 0 violaciones duras</div>
      <label><strong>Ver por:</strong>
        <select id="vista" [(ngModel)]="vistaDocente">
          <option value="">Todos los docentes</option>
          <option *ngFor="let d of docentesIds" [value]="d">Docente {{ d }}</option>
        </select>
      </label>
    </div>

    <div class="toolbar">
      <div class="tb-row">
        <div class="tb-mod">
          <strong>1. Carga de datos:</strong>
          <button class="btn-dark" type="button" (click)="cargarJson()">Cargar JSON (20 cursos / 12 doc / 12 aulas)</button>
        </div>
        <span class="tb-sep">|</span>
        <div class="tb-mod">
          <strong>2. Parámetros (RF6):</strong>
          <label>Cupo: <input type="number" [(ngModel)]="cupo" style="width:48px"></label>
          <label>RB1:
            <select [(ngModel)]="rb1">
              <option value="aviso">Aviso (blanda)</option>
              <option value="dura">Dura (bloquea)</option>
            </select>
          </label>
          <label>Inicio:
            <select [(ngModel)]="inicio">
              <option [ngValue]="7">07:00</option>
              <option [ngValue]="6">06:00</option>
            </select>
          </label>
          <button class="btn-green" type="button" (click)="regenerar()">✓ Regenerar (RF1)</button>
        </div>
      </div>
      <div class="tb-row">
        <div class="tb-mod">
          <strong>3. Reasignar aula (RF3):</strong>
          <select [(ngModel)]="sesionRf3">
            <option *ngFor="let s of sesiones" [value]="s.id">
              {{ s.id }} · {{ cursos[s.c].n }} ({{ dias[s.dia] }} {{ s.h }}-{{ s.h + 2 }} · {{ s.a }})
            </option>
          </select>
          <span>→</span>
          <select [(ngModel)]="aulaRf3">
            <option *ngFor="let k of aulasIds" [value]="k">
              Aula {{ k }} ({{ aulas[k].t }} · {{ aulas[k].c }} puestos)
            </option>
          </select>
          <button type="button" (click)="cambiarAula()">Cambiar aula</button>
        </div>
        <span class="tb-sep">|</span>
        <div class="tb-mod">
          <strong>4. Exportar:</strong>
          <button class="btn-dark" type="button" (click)="exportar('csv')">Exportar CSV</button>
          <button type="button" (click)="exportar('json')">Exportar JSON</button>
        </div>
      </div>
    </div>

    <div class="wrap">
      <div class="panel">
        <h2>Sin ubicar (RF5)</h2>
        <div id="pend">
          <div class="card pend" draggable="true" data-id="K09#0" (dragstart)="onDragStart($event, 'K09#0')">
            Bases de Datos (K09)<small>44 inscritos · req. cómputo (máx 34) · RD5</small>
          </div>
        </div>
        <div class="cat-box">
          <strong>Catálogo activo:</strong>
          <div>• 20 asignaturas (41 sesiones de 2h)</div>
          <div>• 12 docentes (TC=8, TQ=6, MT=4, UQ=2)</div>
          <div>• 12 aulas (5 estándar, 4 cómputo, 3 móviles)</div>
          <div>• 32 bloques semanales (alm 12:00-13:00)</div>
          <div>• Prefiltro O(1) + Retroceso 1 nivel activo</div>
        </div>
      </div>
      <div>
        <table id="grid">
          <tr>
            <th class="h">Bloque</th>
            <th *ngFor="let d of dias">{{ d }}</th>
          </tr>
          <tr *ngFor="let h of bloques">
            <td class="h">{{ h }}:00-<br>{{ h + 2 }}:00</td>
            <td *ngFor="let d of dias; let dia = index"
                [class]="claseCelda(dia, h)"
                [class.over]="celdaSobre === dia + '-' + h"
                [attr.data-dia]="dia"
                [attr.data-h]="h"
                (dragover)="onDragOver($event, dia, h)"
                (dragleave)="celdaSobre = null"
                (drop)="onDrop($event, dia, h)">
              <div *ngFor="let s of sesionesEn(dia, h)"
                   class="card"
                   draggable="true"
                   [attr.data-id]="s.id"
                   (dragstart)="onDragStart($event, s.id)">
                {{ cursos[s.c].n }}<small>{{ s.d }} · Aula {{ s.a }}</small>
              </div>
            </td>
          </tr>
        </table>
      </div>
    </div>
    <div class="msg" [ngClass]="tipoMsg + '-box'" id="msg" [innerHTML]="mensajeHtml"></div>
    <div class="log" id="log">
      <div *ngFor="let item of bitacora">{{ item }}</div>
    </div>
  `
})
export class WireframeP1Component {
  readonly dias = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  bloques = [7, 9, 13, 15, 17, 19];
  readonly docentes: Record<string, { desde?: number }> = { D1: {}, D3: {}, D4: { desde: 16 }, D5: {} };
  readonly docentesIds = Object.keys(this.docentes);
  readonly aulas: Record<string, { t: string; c: number }> = {
    E1: { t: 'estandar', c: 45 }, E2: { t: 'estandar', c: 45 }, E3: { t: 'estandar', c: 45 },
    E4: { t: 'estandar', c: 50 }, E5: { t: 'estandar', c: 50 },
    C1: { t: 'computo', c: 34 }, C2: { t: 'computo', c: 34 }, C3: { t: 'computo', c: 32 }, C4: { t: 'computo', c: 30 },
    M1: { t: 'movil', c: 44 }, M2: { t: 'movil', c: 40 }, M3: { t: 'movil', c: 40 }
  };
  readonly aulasIds = Object.keys(this.aulas);
  readonly cursos: Record<string, { n: string; f: number; t?: string; req: string; ins: number }> = {
    K01: { n: 'Programación I', f: 4, req: 'computo', ins: 30 },
    K02: { n: 'Cálculo Diferencial', f: 3, req: 'estandar', ins: 42 },
    K03: { n: 'Física Mecánica', f: 3, req: 'estandar', ins: 44 },
    K05: { n: 'Ing. de Software I', f: 3, req: 'movil', ins: 40 },
    K08: { n: 'Redes de Datos I', f: 2, req: 'computo', ins: 34 },
    K09: { n: 'Bases de Datos', f: 2, t: 'computo', req: 'computo', ins: 44 }
  };

  sesiones: SesionP1[] = this.clonarBase();
  vistaDocente = '';
  cupo = 40;
  rb1: 'aviso' | 'dura' = 'aviso';
  inicio = 7;
  sesionRf3 = 'K03#0';
  aulaRf3 = 'E5';
  intentos = 1;
  dragId: string | null = null;
  celdaSobre: string | null = null;
  tipoMsg: 'ok' | 'warn' | 'bad' = 'ok';
  mensajeHtml = '<span class="ok">Carga exitosa (POST /api/horario/cargar): archivo datos_prueba_20cursos.json validado (20 asignaturas, 12 docentes, 12 aulas, 32 bloques). GeneradorVoraz ubicó 39/41 sesiones en 0,52 ms con 0 violaciones duras.</span>';
  bitacora: string[] = ['Intento 1: Carga JSON: datos_prueba_20cursos.json -> 39/41 sesiones ubicadas, K09 reportado con causa RD5, Auditor=0 violaciones'];

  private clonarBase(): SesionP1[] {
    return [
      { id: 'K01#0', c: 'K01', d: 'D1', a: 'C1', dia: 0, h: 7 },
      { id: 'K01#1', c: 'K01', d: 'D1', a: 'C1', dia: 1, h: 7 },
      { id: 'K01#2', c: 'K01', d: 'D1', a: 'C1', dia: 3, h: 7 },
      { id: 'K01#3', c: 'K01', d: 'D1', a: 'C1', dia: 4, h: 7 },
      { id: 'K02#0', c: 'K02', d: 'D5', a: 'E3', dia: 0, h: 9 },
      { id: 'K02#1', c: 'K02', d: 'D5', a: 'E3', dia: 2, h: 9 },
      { id: 'K02#2', c: 'K02', d: 'D5', a: 'E3', dia: 4, h: 9 },
      { id: 'K03#0', c: 'K03', d: 'D4', a: 'E4', dia: 0, h: 17 },
      { id: 'K03#1', c: 'K03', d: 'D4', a: 'E4', dia: 2, h: 17 },
      { id: 'K03#2', c: 'K03', d: 'D4', a: 'E4', dia: 4, h: 17 },
      { id: 'K05#0', c: 'K05', d: 'D5', a: 'M1', dia: 1, h: 15 },
      { id: 'K05#1', c: 'K05', d: 'D5', a: 'M1', dia: 3, h: 15 },
      { id: 'K05#2', c: 'K05', d: 'D5', a: 'M1', dia: 5, h: 7 },
      { id: 'K08#0', c: 'K08', d: 'D3', a: 'C2', dia: 1, h: 13 },
      { id: 'K08#1', c: 'K08', d: 'D3', a: 'C2', dia: 3, h: 13 }
    ];
  }

  cargarJson(): void {
    this.sesiones = this.clonarBase();
    this.intentos++;
    this.tipoMsg = 'ok';
    this.mensajeHtml = '<span class="ok">Carga exitosa (POST /api/horario/cargar): archivo datos_prueba_20cursos.json validado (20 asignaturas, 12 docentes, 12 aulas, 32 bloques). GeneradorVoraz ubicó 39/41 sesiones en 0,52 ms con 0 violaciones duras.</span>';
    this.bitacora.unshift(`Intento ${this.intentos}: Carga JSON: datos_prueba_20cursos.json -> 39/41 sesiones ubicadas, K09 reportado con causa RD5, Auditor=0 violaciones`);
  }

  regenerar(): void {
    this.sesiones = this.clonarBase();
    this.intentos++;
    this.tipoMsg = 'ok';
    const nBloq = this.inicio === 6 ? 37 : 32;
    const ub = this.cupo <= 30 ? 41 : 39;
    this.mensajeHtml = `<span class="ok">Horario regenerado (POST /api/horario/reiniciar): cupo=${this.cupo}, RB1=${this.rb1}, bloques=${nBloq}. Ubicadas: ${ub}/41 · Auditor: 0 violaciones.</span>`;
    this.bitacora.unshift(`Intento ${this.intentos}: Regenerar (RF1/RF6): cupo=${this.cupo}, RB1=${this.rb1} -> ${ub}/41 sesiones ubicadas`);
  }

  cambiarAula(): void {
    const s = this.sesiones.find(x => x.id === this.sesionRf3);
    if (!s) return;
    const c = this.cursos[s.c];
    const a = this.aulas[this.aulaRf3];
    this.intentos++;
    if (a.t !== c.req || a.c < c.ins) {
      this.tipoMsg = 'bad';
      this.mensajeHtml = `<div class="bad">Rechazado (RF3 · RD5): El aula ${this.aulaRf3} es de tipo ${a.t} (${a.c} puestos) y ${c.n} requiere aula tipo ${c.req} (${c.ins} inscritos).</div>`;
      this.bitacora.unshift(`Intento ${this.intentos}: Cambio de aula ${s.id} -> ${this.aulaRf3} rechazado (RD5)`);
      return;
    }
    const prev = s.a;
    s.a = this.aulaRf3;
    this.tipoMsg = 'ok';
    this.mensajeHtml = `<span class="ok">Aceptado (RF3): Aula de ${c.n} (${s.id}) reasignada de ${prev} a ${this.aulaRf3} (${a.t}, ${a.c} puestos). Validación en 0,05 ms · Auditor: 0 violaciones.</span>`;
    this.bitacora.unshift(`Intento ${this.intentos}: Cambio de aula ${s.id}: ${prev} -> ${this.aulaRf3} aceptado (RF3)`);
  }

  exportar(formato: 'csv' | 'json'): void {
    this.intentos++;
    this.tipoMsg = 'ok';
    if (formato === 'csv') {
      this.mensajeHtml = '<span class="ok">Exportación completada (GET /api/horario/exportar?formato=csv): archivo horario_bosquescheduler.csv generado con 39 sesiones asignadas, encabezado normalizado y 0 colisiones (Auditor OK).</span>';
      this.bitacora.unshift(`Intento ${this.intentos}: Exportación CSV: horario_bosquescheduler.csv descargado (40 líneas: 1 encabezado + 39 sesiones)`);
    } else {
      this.mensajeHtml = '<span class="ok">Exportación completada (GET /api/horario/exportar?formato=json): archivo horario_bosquescheduler.json descargado (Auditor=0 violaciones).</span>';
      this.bitacora.unshift(`Intento ${this.intentos}: Exportación JSON: horario_bosquescheduler.json descargado`);
    }
  }

  sesionesEn(dia: number, h: number): SesionP1[] {
    return this.sesiones.filter(s => s.dia === dia && s.h === h);
  }

  ocupado(d: string, dia: number, h: number): boolean {
    return this.sesiones.some(s => s.d === d && s.dia === dia && s.h === h);
  }

  claseCelda(dia: number, h: number): string {
    let cls = 'drop';
    if (dia === 5 && h >= 13) cls += ' sab';
    if (this.vistaDocente) {
      const doc = this.docentes[this.vistaDocente];
      if ((doc?.desde && h < doc.desde) || this.ocupado(this.vistaDocente, dia, h)) {
        cls += ' nodoc';
      }
    }
    return cls;
  }

  onDragStart(ev: DragEvent, id: string): void {
    this.dragId = id;
    ev.dataTransfer?.setData('text/plain', id);
  }

  onDragOver(ev: DragEvent, dia: number, h: number): void {
    ev.preventDefault();
    this.celdaSobre = `${dia}-${h}`;
  }

  onDrop(ev: DragEvent, dia: number, h: number): void {
    ev.preventDefault();
    this.celdaSobre = null;
    const id = this.dragId ?? ev.dataTransfer?.getData('text/plain') ?? null;
    if (id) {
      this.intentar(id, dia, h);
    }
    this.dragId = null;
  }

  intentar(id: string, dia: number, h: number): void {
    const s = this.sesiones.find(x => x.id === id) ?? { id, c: 'K09', d: 'D3', a: 'C1', dia: -1, h: 0 };
    const r = this.validar(s, dia, h);
    this.intentos++;
    const txt = `${this.cursos[s.c].n} → ${this.dias[dia]} ${h}-${h + 2}`;
    if (r.duras.length) {
      this.tipoMsg = 'bad';
      this.mensajeHtml = r.duras.map(t => `<div class="bad">Rechazado. ${t}</div>`).join('') + '<div>La sesión permanece donde estaba.</div>';
      this.bitacora.unshift(`Intento ${this.intentos}: ${txt}: rechazado`);
    } else {
      if (this.sesiones.includes(s)) {
        s.dia = dia;
        s.h = h;
      }
      this.tipoMsg = r.avisos.length ? 'warn' : 'ok';
      this.mensajeHtml = '<div class="ok">Aceptado.</div>' + r.avisos.map(t => `<div class="warn">Aviso. ${t}</div>`).join('');
      this.bitacora.unshift(`Intento ${this.intentos}: ${txt}: aceptado${r.avisos.length ? ' con aviso' : ''}`);
    }
  }

  validar(s: SesionP1, dia: number, h: number): { duras: string[]; avisos: string[] } {
    const duras: string[] = [];
    const avisos: string[] = [];
    const c = this.cursos[s.c];
    const d = this.docentes[s.d] ?? {};
    if (dia === 5 && h >= 13) {
      duras.push('RD4: el sábado solo hay franja de 7:00 a 13:00 h.');
    }
    if (c.t && c.ins) {
      let max = 0;
      for (const k of Object.keys(this.aulas)) {
        if (this.aulas[k].t === c.t) max = Math.max(max, this.aulas[k].c);
      }
      if (max < c.ins) {
        duras.push(`RD5: ninguna aula de tipo ${c.t} tiene ${c.ins} puestos (el máximo es ${max}). Opciones: cambiar el tope del grupo o dividirlo.`);
      }
    } else {
      if (d.desde && h < d.desde) {
        duras.push(`RD7: ${s.d} solo dicta después de las ${d.desde}:00 h.`);
      }
      const otras = this.sesiones.filter(x => x.id !== s.id && x.dia === dia && x.h === h);
      otras.forEach(x => {
        if (x.d === s.d) duras.push(`RD1: ${s.d} ya dicta ${this.cursos[x.c].n} el ${this.dias[dia]} de ${h} a ${h + 2}.`);
        if (x.a === s.a) duras.push(`RD2: el aula ${s.a} ya está ocupada por ${this.cursos[x.c].n} en ese bloque.`);
      });
      if (c.f >= 2) {
        const diasCurso = this.sesiones.filter(x => x.c === s.c && x.id !== s.id).map(x => x.dia);
        if (diasCurso.some(x => x === dia)) {
          duras.push(`RB1: ${c.n} ya tiene otra sesión programada el ${this.dias[dia]}. Se prohíbe más de una sesión diaria del mismo curso.`);
        } else if (c.f <= 3 && diasCurso.some(x => Math.abs(x - dia) === 1)) {
          avisos.push(`RB1: ${c.n} quedaría con sesiones en días consecutivos. El movimiento se permite.`);
        }
      }
    }
    return { duras, avisos };
  }
}
