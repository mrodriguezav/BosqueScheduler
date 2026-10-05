package co.edu.unbosque.horarios.model;

public class Aula {
    public final String id;
    public final String tipo;
    public final int capacidad;

    public Aula(String id, String tipo, int capacidad) {
        this.id = id;
        this.tipo = tipo;
        this.capacidad = capacidad;
    }
}
