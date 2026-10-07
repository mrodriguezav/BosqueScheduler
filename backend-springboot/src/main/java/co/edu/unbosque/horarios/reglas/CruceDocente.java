package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class CruceDocente extends Regla {
    public CruceDocente() {
        super("RD1", "dura", false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        String otra = h.docenteEn(c.docenteId, c.bloqueId);
        return (otra != null && !otra.equals(c.id))
            ? violacion("El docente " + c.docenteId + " ya dicta " + otra + " en ese bloque")
            : List.of();
    }
}
