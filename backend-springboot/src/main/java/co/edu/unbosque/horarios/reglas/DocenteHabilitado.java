package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class DocenteHabilitado extends Regla {
    public DocenteHabilitado() {
        super("RD3", "dura", false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Curso curso = h.getCtx().cursos.get(c.cursoId);
        return curso.habilitados.contains(c.docenteId)
            ? List.of()
            : violacion("El docente " + c.docenteId + " no está habilitado para " + curso.nombre);
    }
}
