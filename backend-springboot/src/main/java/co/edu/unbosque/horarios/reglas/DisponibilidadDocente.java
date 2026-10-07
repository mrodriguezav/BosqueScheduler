package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Docente;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class DisponibilidadDocente extends Regla {
    public DisponibilidadDocente() {
        super("RD7", "dura", false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Docente d = h.getCtx().docentes.get(c.docenteId);
        Bloque b = h.getCtx().bloquePorId.get(c.bloqueId);
        return (b != null && h.getCtx().admiteDocente(d, b))
            ? List.of()
            : violacion("El docente " + d.id + " no está disponible en ese bloque");
    }
}
