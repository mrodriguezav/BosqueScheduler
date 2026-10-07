package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class FranjaHoraria extends Regla {
    public FranjaHoraria() {
        super("RD4", "dura", false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Bloque b = h.getCtx().bloquePorId.get(c.bloqueId);
        Configuracion.RangoHora a = h.getCtx().cfg.almuerzo;
        if (b == null) {
            return violacion("El bloque está fuera de la franja horaria declarada");
        }
        return (b.inicio < a.fin && b.fin > a.inicio)
            ? violacion("El bloque cruza la hora de almuerzo")
            : List.of();
    }
}
