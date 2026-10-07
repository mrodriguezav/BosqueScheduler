package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.config.Configuracion;
import java.util.List;

/**
 * Registro de reglas: el único lugar que se edita para agregar o reclasificar una restricción (RNF3).
 * El orden va de la regla más barata a la más costosa, porque el generador corta en la primera violación.
 */
public class CatalogoReglas {
    public static List<Regla> crearReglas(Configuracion cfg) {
        return List.of(
            new DocenteHabilitado(),
            new DisponibilidadDocente(),
            new FranjaHoraria(),
            new DuracionSesion(),
            new AulaCompatible(),
            new CruceDocente(),
            new CruceAula(),
            new TopeDedicacion(),
            new DiasConsecutivos(cfg.severidades.getOrDefault("RB1", "aviso"))
        );
    }
}
