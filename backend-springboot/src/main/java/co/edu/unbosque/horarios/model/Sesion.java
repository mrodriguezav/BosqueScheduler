package co.edu.unbosque.horarios.model;

/**
 * Sesión asignada o candidata. Guarda referencias por identificador (cursoId, docenteId, aulaId, bloqueId).
 */
public class Sesion {
    public final String id;
    public final String cursoId;
    public final String docenteId;
    public final String aulaId;
    public final String bloqueId;

    public Sesion(String id, String cursoId, String docenteId, String aulaId, String bloqueId) {
        this.id = id;
        this.cursoId = cursoId;
        this.docenteId = docenteId;
        this.aulaId = aulaId;
        this.bloqueId = bloqueId;
    }

    public Sesion conBloqueYAula(String nuevoBloqueId, String nuevaAulaId) {
        return new Sesion(this.id, this.cursoId, this.docenteId, nuevaAulaId != null ? nuevaAulaId : this.aulaId, nuevoBloqueId);
    }
}
