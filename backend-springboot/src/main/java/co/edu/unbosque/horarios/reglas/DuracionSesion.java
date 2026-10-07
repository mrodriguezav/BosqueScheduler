package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class DuracionSesion extends Regla {
    public DuracionSesion() {
        super("RD6", "dura", false);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Bloque b = h.getCtx().bloquePorId.get(c.bloqueId);
        return (b != null && (b.fin - b.inicio) == h.getCtx().cfg.duracionSesion)
            ? List.of()
            : violacion("La sesión debe ocupar exactamente un bloque de la duración configurada");
    }
}
