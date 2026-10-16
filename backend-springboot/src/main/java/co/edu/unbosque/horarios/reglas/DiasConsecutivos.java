package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class DiasConsecutivos extends Regla {
    public DiasConsecutivos(String severidad) {
        super("RB1", severidad, false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Curso curso = h.getCtx().cursos.get(c.cursoId);
        if (curso.frecuencia < 2) return List.of();
        Bloque b = h.getCtx().bloquePorId.get(c.bloqueId);
        if (b == null) return List.of();
        boolean tieneConsecutivo = false;
        for (Sesion s : h.sesionesDeCurso(c.cursoId)) {
            if (s.id.equals(c.id)) continue;
            Bloque bOtra = h.getCtx().bloquePorId.get(s.bloqueId);
            if (bOtra != null) {
                if (bOtra.dia == b.dia) {
                    return List.of(new Violacion(this.codigo, "dura",
                        curso.nombre + " ya tiene otra sesión programada el día " + b.nombreDia + ". Se prohíbe más de una sesión diaria del mismo curso."));
                }
                if (curso.frecuencia <= 3 && Math.abs(bOtra.dia - b.dia) == 1) {
                    tieneConsecutivo = true;
                }
            }
        }
        if (tieneConsecutivo) {
            return violacion(curso.nombre + " quedaría con sesiones en días consecutivos");
        }
        return List.of();
    }
}
