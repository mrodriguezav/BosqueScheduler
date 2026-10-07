package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

/**
 * Interfaz común de regla (patrón Strategy). Cada regla recibe la candidata
 * y el estado del horario, y devuelve una lista de violaciones (vacía si cumple).
 */
public abstract class Regla {
    public final String codigo;
    public final String severidad;
    public final boolean usaAula;

    public Regla(String codigo, String severidad, boolean usaAula) {
        this.codigo = codigo;
        this.severidad = severidad;
        this.usaAula = usaAula;
    }

    public Regla(String codigo, String severidad) {
        this(codigo, severidad, false);
    }

    public abstract List<Violacion> evaluar(Sesion candidata, Horario horario);

    protected List<Violacion> violacion(String motivo) {
        return List.of(new Violacion(this.codigo, this.severidad, motivo));
    }
}
