package co.edu.unbosque.horarios.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parámetros de planeación (RF6). Cambiar un valor aquí no exige tocar el generador ni el validador.
 */
public class Configuracion {

    public static class RangoHora {
        public int inicio;
        public int fin;

        public RangoHora(int inicio, int fin) {
            this.inicio = inicio;
            this.fin = fin;
        }

        public RangoHora copiar() {
            return new RangoHora(this.inicio, this.fin);
        }
    }

    public static class FranjaConfig {
        public RangoHora semana;
        public RangoHora sabado;

        public FranjaConfig(RangoHora semana, RangoHora sabado) {
            this.semana = semana;
            this.sabado = sabado;
        }

        public FranjaConfig copiar() {
            return new FranjaConfig(semana.copiar(), sabado.copiar());
        }
    }

    public FranjaConfig franja;
    public RangoHora almuerzo;
    public int duracionSesion;
    public int cupoMaximo;
    public double tolerancia;
    public Map<String, Integer> dedicacion;
    public Map<String, String> severidades;

    public Configuracion() {
        this.franja = new FranjaConfig(new RangoHora(7, 22), new RangoHora(7, 13));
        this.almuerzo = new RangoHora(12, 13);
        this.duracionSesion = 2;
        this.cupoMaximo = 40;
        this.tolerancia = 0.10;
        this.dedicacion = new LinkedHashMap<>();
        this.dedicacion.put("TC", 8);
        this.dedicacion.put("TQ", 6);
        this.dedicacion.put("MT", 4);
        this.dedicacion.put("UQ", 2);
        this.severidades = new LinkedHashMap<>();
        this.severidades.put("RB1", "aviso");
    }

    public static Configuracion porDefecto() {
        return new Configuracion();
    }

    public Configuracion copiar() {
        Configuracion c = new Configuracion();
        c.franja = this.franja.copiar();
        c.almuerzo = this.almuerzo.copiar();
        c.duracionSesion = this.duracionSesion;
        c.cupoMaximo = this.cupoMaximo;
        c.tolerancia = this.tolerancia;
        c.dedicacion = new LinkedHashMap<>(this.dedicacion);
        c.severidades = new LinkedHashMap<>(this.severidades);
        return c;
    }
}
