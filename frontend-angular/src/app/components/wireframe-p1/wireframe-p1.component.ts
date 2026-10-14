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
      <h1>Horario semanal (propuesta generada — Angular P1 + Spring Boot)</h1>
      <label>Ver por:
        <select id="vista" [(ngModel)]="vistaDocente">
          <option value="">Todos</option>
          <option *ngFor="let d of docentesIds" [value]="d">Docente {{ d }}</option>
        </select>
      </label>
    </div>
    <div class="wrap">
      <div class="panel">
        <h2>Sin ubicar (RF5)</h2>
        <div id="pend">
          <div class="card pend" draggable="true" data-id="K09#0" (dragstart)="onDragStart($event, 'K09#0')">
            Bases de Datos<small>44 inscritos · sin ubicar</small>
          </div>
        </div>
      </div>
      <div>
        <table id="grid">
          <tr>
            <td class="h"></td>
            <th *ngFor="let d of dias">{{ d }}</th>
          </tr>
          <tr *ngFor="let h of bloques">
            <td class="h">{{ h }}-{{ h + 2 }}</td>
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
                {{ cursos[s.c].n }}<small>{{ s.d }} · {{ s.a }}</small>
              </div>
            </td>
          </tr>
        </table>
      </div>
    </div>
    <div class="msg" id="msg" [innerHTML]="mensajeHtml"></div>
    <div class="log" id="log">
      <div *ngFor="let item of bitacora">{{ item }}</div>
    </div>
  `
})
export class WireframeP1Component {
  readonly dias = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  readonly bloques = [7, 9, 13, 15, 17, 19];
  readonly docentes: Record<string, { desde?: number }> = { D1: {}, D3: {}, D4: { desde: 16 }, D5: {} };
  readonly docentesIds = Object.keys(this.docentes);
  readonly aulas: Record<string, { t: string; c: number }> = {
    C1: { t: 'computo', c: 34 }, C2: { t: 'computo', c: 34 },
    E3: { t: 'estandar', c: 45 }, E4: { t: 'estandar', c: 50 }, M1: { t: 'movil', c: 44 }
  };
  readonly cursos: Record<string, { n: string; f: number; t?: string; ins?: number }> = {
    K01: { n: 'Programación I', f: 4 },
    K02: { n: 'Cálculo Diferencial', f: 3 },
    K03: { n: 'Física Mecánica', f: 3 },
    K05: { n: 'Ing. de Software I', f: 3 },
    K08: { n: 'Redes de Datos I', f: 2 },
    K09: { n: 'Bases de Datos', f: 2, t: 'computo', ins: 44 }
  };

  sesiones: SesionP1[] = [
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

  vistaDocente = '';
  intentos = 0;
  dragId: string | null = null;
  celdaSobre: string | null = null;
  mensajeHtml = '<span class="ok">Arrastre una tarjeta a otro bloque.</span>';
  bitacora: string[] = [];

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
      this.mensajeHtml = r.duras.map(t => `<div class="bad">Rechazado. ${t}</div>`).join('') + '<div>La sesión permanece donde estaba.</div>';
      this.bitacora.unshift(`Intento ${this.intentos}: ${txt}: rechazado`);
    } else {
      if (this.sesiones.includes(s)) {
        s.dia = dia;
        s.h = h;
      }
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
