export interface Bloque {
  id: string;
  dia: number;
  nombreDia: string;
  inicio: number;
  fin: number;
  orden: number;
}

export interface Disponibilidad {
  dias?: number[] | null;
  desde?: number | null;
  hasta?: number | null;
}

export interface Docente {
  id: string;
  dedicacion: 'TC' | 'TQ' | 'MT' | 'UQ';
  disponibilidad?: Disponibilidad | null;
  carga?: number;
  tope?: number;
}

export interface Aula {
  id: string;
  tipo: 'estandar' | 'computo' | 'movil';
  capacidad: number;
}

export interface Curso {
  id: string;
  nombre: string;
  tipoAula: 'estandar' | 'computo' | 'movil';
  inscritos: number;
  frecuencia: number;
  habilitados?: string[];
}

export interface Sesion {
  id: string;
  cursoId: string;
  cursoNombre?: string;
  docenteId: string;
  aulaId: string;
  bloqueId: string;
}

export interface Violacion {
  regla: string;
  severidad: 'dura' | 'aviso';
  motivo: string;
}

export interface CursoSinUbicar {
  cursoId: string;
  nombre: string;
  inscritos?: number;
  causa: string;
}

export interface ResultadoAuditoria {
  crucesDocente: number;
  crucesAula: number;
  total: number;
}

export interface EstadoHorarioDto {
  ubicadas: number;
  totalSesiones: number;
  evaluaciones: number;
  retrocesos: number;
  bloques: Bloque[];
  cursos: Curso[];
  docentes: Docente[];
  aulas: Aula[];
  sesiones: Sesion[];
  sinUbicar: CursoSinUbicar[];
  auditoria: ResultadoAuditoria;
}

export interface RespuestaMovimientoDto {
  aceptado: boolean;
  tiempoMs: number;
  violaciones: Violacion[];
  auditoria: ResultadoAuditoria;
  estado: EstadoHorarioDto;
}
