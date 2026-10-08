package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.datos.DatosEjemplo.ConjuntoDatos;
import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.reglas.CatalogoReglas;

/**
 * Fábrica central equivalente a crearSistema() en src/index.js.
 */
public class SistemaHorarios {
    public final Contexto ctx;
    public final ValidadorRestricciones validador;
    public final GeneradorVoraz generador;

    public SistemaHorarios(ConjuntoDatos datos, Configuracion cfg, boolean prefiltro) {
        this.ctx = new Contexto(datos != null ? datos : DatosEjemplo.base(), cfg != null ? cfg : Configuracion.porDefecto());
        this.validador = new ValidadorRestricciones(CatalogoReglas.crearReglas(this.ctx.cfg));
        this.generador = new GeneradorVoraz(this.validador, this.ctx, prefiltro);
    }

    public static SistemaHorarios crearSistema() {
        return new SistemaHorarios(DatosEjemplo.base(), Configuracion.porDefecto(), true);
    }

    public static SistemaHorarios crearSistema(ConjuntoDatos datos, Configuracion cfg, boolean prefiltro) {
        return new SistemaHorarios(datos, cfg, prefiltro);
    }

    public Horario nuevoHorario() {
        return new Horario(this.ctx);
    }
}
