package co.edu.unbosque.horarios.model;

import java.util.List;

public class Docente {
    public static class Disponibilidad {
        public final List<Integer> dias;
        public final Integer desde;
        public final Integer hasta;

        public Disponibilidad(List<Integer> dias, Integer desde, Integer hasta) {
            this.dias = dias;
            this.desde = desde;
            this.hasta = hasta;
        }
    }

    public final String id;
    public final String dedicacion;
    public final Disponibilidad disponibilidad;

    public Docente(String id, String dedicacion, Disponibilidad disponibilidad) {
        this.id = id;
        this.dedicacion = dedicacion;
        this.disponibilidad = disponibilidad;
    }

    public Docente(String id, String dedicacion) {
        this(id, dedicacion, null);
    }
}
