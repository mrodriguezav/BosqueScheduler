package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.reglas.Regla;
import java.util.ArrayList;
import java.util.List;

/**
 * El generador y el arrastrar y soltar consumen este mismo validador (decisión U3).
 */
public class ValidadorRestricciones {
    public final List<Regla> reglas;

    public ValidadorRestricciones(List<Regla> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    // Ruta de la interfaz: devuelve todas las violaciones con su motivo (RF2, RF4).
    public List<Violacion> evaluar(Sesion candidata, Horario horario) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Validador.evaluar");
        List<Violacion> out = new ArrayList<>();
        for (Regla r : this.reglas) {
            List<Violacion> v = r.evaluar(candidata, horario);
            if (!v.isEmpty()) {
                out.addAll(v);
            }
        }
        return out;
    }

    // Ruta del generador: corta en la primera violación dura.
    public boolean esValida(Sesion candidata, Horario horario) {
        for (Regla r : this.reglas) {
            List<Violacion> v = r.evaluar(candidata, horario);
            if (v.stream().anyMatch(x -> "dura".equals(x.severidad))) return false;
            if ("dura".equals(r.severidad) && !v.isEmpty()) return false;
        }
        return true;
    }

    // Reglas duras que no usan el aula (se evalúan una vez por docente y bloque).
    public boolean esValidaSinAula(Sesion candidata, Horario horario) {
        for (Regla r : this.reglas) {
            if (r.usaAula) continue;
            List<Violacion> v = r.evaluar(candidata, horario);
            if (v.stream().anyMatch(x -> "dura".equals(x.severidad))) return false;
            if ("dura".equals(r.severidad) && !v.isEmpty()) return false;
        }
        return true;
    }

    // Reglas duras que sí usan el aula (se evalúan por cada aula candidata).
    public boolean esValidaConAula(Sesion candidata, Horario horario) {
        for (Regla r : this.reglas) {
            if (!r.usaAula) continue;
            List<Violacion> v = r.evaluar(candidata, horario);
            if (v.stream().anyMatch(x -> "dura".equals(x.severidad))) return false;
            if ("dura".equals(r.severidad) && !v.isEmpty()) return false;
        }
        return true;
    }
}
