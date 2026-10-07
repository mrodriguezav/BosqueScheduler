package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class CruceAula extends Regla {
    public CruceAula() {
        super("RD2", "dura", true);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        String otra = h.aulaEn(c.aulaId, c.bloqueId);
        return (otra != null && !otra.equals(c.id))
            ? violacion("El aula " + c.aulaId + " ya está ocupada por " + otra + " en ese bloque")
            : List.of();
    }
}
