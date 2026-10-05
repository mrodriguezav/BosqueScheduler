package co.edu.unbosque.horarios.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Único punto de mutación del estado: colocar, quitar y mover mantienen
 * sincronizados el mapa de ocupación de docentes y aulas (decisión U2, RNF2).
 * En Java 21 los mapas internos son private final para impedir mutaciones externas en tiempo de compilación.
 */
public class Horario {
    private final Contexto ctx;
    private final Map<String, Sesion> sesiones = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> ocDocente = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> ocAula = new LinkedHashMap<>();
    private final Map<String, Integer> carga = new LinkedHashMap<>();
    private final Map<String, Set<String>> porCurso = new LinkedHashMap<>();

    public interface EscuchaTraza {
        void registrar(String evento);
    }

    public static EscuchaTraza trazaActiva = null;

    public Horario(Contexto ctx) {
        this.ctx = ctx;
    }

    public Contexto getCtx() {
        return ctx;
    }

    public Map<String, Sesion> getSesiones() {
        return Collections.unmodifiableMap(sesiones);
    }

    public Sesion obtenerSesion(String id) {
        return sesiones.get(id);
    }

    public int totalSesiones() {
        return sesiones.size();
    }

    public String docenteEn(String docenteId, String bloqueId) {
        if (trazaActiva != null) trazaActiva.registrar("Horario.consulta");
        Map<String, String> m = ocDocente.get(docenteId);
        return m != null ? m.get(bloqueId) : null;
    }

    public String aulaEn(String aulaId, String bloqueId) {
        if (trazaActiva != null) trazaActiva.registrar("Horario.consulta");
        Map<String, String> m = ocAula.get(aulaId);
        return m != null ? m.get(bloqueId) : null;
    }

    public int cargaDe(String docenteId) {
        return carga.getOrDefault(docenteId, 0);
    }

    public List<Sesion> sesionesDeCurso(String cursoId) {
        Set<String> ids = porCurso.get(cursoId);
        if (ids == null || ids.isEmpty()) return List.of();
        List<Sesion> res = new ArrayList<>(ids.size());
        for (String id : ids) {
            Sesion s = sesiones.get(id);
            if (s != null) res.add(s);
        }
        return res;
    }

    public void colocar(Sesion s) {
        if (trazaActiva != null) trazaActiva.registrar("Horario.colocar");
        sesiones.put(s.id, s);
        ocDocente.computeIfAbsent(s.docenteId, k -> new LinkedHashMap<>()).put(s.bloqueId, s.id);
        ocAula.computeIfAbsent(s.aulaId, k -> new LinkedHashMap<>()).put(s.bloqueId, s.id);
        carga.put(s.docenteId, cargaDe(s.docenteId) + 1);
        porCurso.computeIfAbsent(s.cursoId, k -> new LinkedHashSet<>()).add(s.id);
    }

    public void quitar(String id) {
        if (trazaActiva != null) trazaActiva.registrar("Horario.quitar");
        Sesion s = sesiones.get(id);
        if (s == null) return;
        Map<String, String> md = ocDocente.get(s.docenteId);
        if (md != null) md.remove(s.bloqueId);
        Map<String, String> ma = ocAula.get(s.aulaId);
        if (ma != null) ma.remove(s.bloqueId);
        carga.put(s.docenteId, cargaDe(s.docenteId) - 1);
        Set<String> sc = porCurso.get(s.cursoId);
        if (sc != null) sc.remove(id);
        sesiones.remove(id);
    }

    public void mover(String id, String bloqueId, String aulaId) {
        if (trazaActiva != null) trazaActiva.registrar("Horario.mover");
        Sesion s = sesiones.get(id);
        if (s == null) return;
        quitar(id);
        colocar(new Sesion(s.id, s.cursoId, s.docenteId, (aulaId != null && !aulaId.isEmpty()) ? aulaId : s.aulaId, bloqueId));
    }
}
