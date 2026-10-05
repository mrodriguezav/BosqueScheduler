package co.edu.unbosque.horarios.model;

public class Violacion {
    public final String regla;
    public final String severidad;
    public final String motivo;

    public Violacion(String regla, String severidad, String motivo) {
        this.regla = regla;
        this.severidad = severidad;
        this.motivo = motivo;
    }
}
