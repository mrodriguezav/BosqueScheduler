package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Docente;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Verificador independiente: recorre solo la lista de sesiones y no usa los índices del Horario
 * ni las clases de regla (RNF2). Devuelve el conteo de violaciones duras.
 */
public class Auditor {

    public static class ResultadoAuditoria {
        public int crucesDocente = 0;
        public int crucesAula = 0;
        public int aulaIncompatible = 0;
        public int noHabilitado = 0;
        public int fueraDeFranja = 0;
        public int noDisponible = 0;
        public int excesoCarga = 0;
        public int total = 0;
    }

    public static ResultadoAuditoria auditar(Horario h) {
        Contexto ctx = h.getCtx();
        ResultadoAuditoria r = new ResultadoAuditoria();
        Set<String> vistoD = new HashSet<>();
        Set<String> vistoA = new HashSet<>();
        Map<String, Integer> carga = new HashMap<>();

        for (Sesion s : h.getSesiones().values()) {
            Bloque b = null;
            for (Bloque x : ctx.bloques) {
                if (x.id.equals(s.bloqueId)) {
                    b = x;
                    break;
                }
            }
            Curso curso = ctx.cursos.get(s.cursoId);
            Aula aula = ctx.aulas.get(s.aulaId);
            Docente doc = ctx.docentes.get(s.docenteId);
            String kd = s.docenteId + "|" + s.bloqueId;
            String ka = s.aulaId + "|" + s.bloqueId;
            if (vistoD.contains(kd)) r.crucesDocente++;
            vistoD.add(kd);
            if (vistoA.contains(ka)) r.crucesAula++;
            vistoA.add(ka);
            if (aula == null || curso == null
                || !aula.tipo.equals(curso.tipoAula) || aula.capacidad < Math.min(curso.inscritos, ctx.topeGrupo)) {
                r.aulaIncompatible++;
            }
            if (curso == null || doc == null || !curso.habilitados.contains(s.docenteId)) {
                r.noHabilitado++;
            }
            if (b == null
                || (b.inicio < ctx.cfg.almuerzo.fin && b.fin > ctx.cfg.almuerzo.inicio)
                || (b.fin - b.inicio) != ctx.cfg.duracionSesion) {
                r.fueraDeFranja++;
            }
            if (b != null && !ctx.admiteDocente(doc, b)) {
                r.noDisponible++;
            }
            carga.put(s.docenteId, carga.getOrDefault(s.docenteId, 0) + 1);
        }

        for (Map.Entry<String, Integer> e : carga.entrySet()) {
            Docente d = ctx.docentes.get(e.getKey());
            int tope = d != null ? ctx.cfg.dedicacion.getOrDefault(d.dedicacion, 0) : 0;
            if (e.getValue() > tope) {
                r.excesoCarga++;
            }
        }
        r.total = r.crucesDocente + r.crucesAula + r.aulaIncompatible + r.noHabilitado
                + r.fueraDeFranja + r.noDisponible + r.excesoCarga;
        return r;
    }
}
