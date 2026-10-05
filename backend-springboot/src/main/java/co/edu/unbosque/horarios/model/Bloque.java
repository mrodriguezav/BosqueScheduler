package co.edu.unbosque.horarios.model;

public class Bloque {
    public final String id;
    public final int dia;
    public final String nombreDia;
    public final int inicio;
    public final int fin;
    public final int orden;

    public Bloque(String id, int dia, String nombreDia, int inicio, int fin, int orden) {
        this.id = id;
        this.dia = dia;
        this.nombreDia = nombreDia;
        this.inicio = inicio;
        this.fin = fin;
        this.orden = orden;
    }
}
