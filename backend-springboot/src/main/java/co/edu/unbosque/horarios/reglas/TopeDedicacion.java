package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Docente;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class TopeDedicacion extends Regla {
    public TopeDedicacion() {
        super("RD8", "dura", false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Docente d = h.getCtx().docentes.get(c.docenteId);
        Sesion previa = h.obtenerSesion(c.id);
        boolean yaCuenta = previa != null && previa.docenteId.equals(c.docenteId);
        int total = h.cargaDe(c.docenteId) + (yaCuenta ? 0 : 1);
        int tope = h.getCtx().topeDocente(d);
        return total <= tope
            ? List.of()
            : violacion("El docente " + d.id + " superaría su tope de " + tope + " sesiones semanales");
    }
}
