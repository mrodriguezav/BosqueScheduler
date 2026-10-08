package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Docente;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;

/**
 * Alternativa sin patrón (B3): una sola función con las condiciones en línea.
 * Solo para comparar rendimiento y cantidad de cambios; el producto usa ValidadorRestricciones.
 */
public class ValidadorMonolitico {
    public static boolean validarMonolitico(Sesion c, Horario h) {
        Contexto ctx = h.getCtx();
        Curso curso = ctx.cursos.get(c.cursoId);
        Docente docente = ctx.docentes.get(c.docenteId);
        Aula aula = ctx.aulas.get(c.aulaId);
        Bloque b = ctx.bloquePorId.get(c.bloqueId);
        if (!curso.habilitados.contains(c.docenteId)) return false;
        if (b == null || !ctx.admiteDocente(docente, b)) return false;
        Configuracion.RangoHora a = ctx.cfg.almuerzo;
        if (b.inicio < a.fin && b.fin > a.inicio) return false;
        if (b.fin - b.inicio != ctx.cfg.duracionSesion) return false;
        if (!aula.tipo.equals(curso.tipoAula) || aula.capacidad < ctx.capacidadRequerida(curso)) return false;
        String od = h.docenteEn(c.docenteId, c.bloqueId);
        if (od != null && !od.equals(c.id)) return false;
        String oa = h.aulaEn(c.aulaId, c.bloqueId);
        if (oa != null && !oa.equals(c.id)) return false;
        Sesion previa = h.obtenerSesion(c.id);
        boolean yaCuenta = previa != null && previa.docenteId.equals(c.docenteId);
        if (h.cargaDe(c.docenteId) + (yaCuenta ? 0 : 1) > ctx.topeDocente(docente)) return false;
        for (Sesion s : h.sesionesDeCurso(c.cursoId)) {
            if (s.id.equals(c.id)) continue;
            Bloque bOtra = ctx.bloquePorId.get(s.bloqueId);
            if (bOtra != null) {
                if (bOtra.dia == b.dia) return false;
                if ("dura".equals(ctx.cfg.severidades.get("RB1")) && curso.frecuencia >= 2 && curso.frecuencia <= 3) {
                    if (Math.abs(bOtra.dia - b.dia) == 1) return false;
                }
            }
        }
        return true;
    }
}
