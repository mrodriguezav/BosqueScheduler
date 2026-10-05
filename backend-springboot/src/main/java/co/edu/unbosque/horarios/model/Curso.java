package co.edu.unbosque.horarios.model;

import java.util.List;

public class Curso {
    public final String id;
    public final String nombre;
    public final String tipoAula;
    public final int inscritos;
    public final int frecuencia;
    public final List<String> habilitados;

    public Curso(String id, String nombre, String tipoAula, int inscritos, int frecuencia, List<String> habilitados) {
        this.id = id;
        this.nombre = nombre;
        this.tipoAula = tipoAula;
        this.inscritos = inscritos;
        this.frecuencia = frecuencia;
        this.habilitados = List.copyOf(habilitados);
    }
}
