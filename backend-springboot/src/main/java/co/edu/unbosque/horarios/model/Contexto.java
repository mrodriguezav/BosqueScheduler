package co.edu.unbosque.horarios.model;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo.ConjuntoDatos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contexto de solo lectura: datos de entrada más parámetros derivados (decisión U4).
 */
public class Contexto {
    public static final String[] NOMBRES_DIA = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};

    public final Configuracion cfg;
    public final List<Bloque> bloques;
    public final Map<String, Bloque> bloquePorId;
    public final Map<String, Curso> cursos;
    public final Map<String, Docente> docentes;
    public final Map<String, Aula> aulas;
    public final List<Aula> aulasLista;
    public final int topeGrupo;

    public Contexto(ConjuntoDatos datos, Configuracion cfg) {
        this.cfg = cfg;
        this.bloques = generarBloques(cfg);
        this.bloquePorId = new LinkedHashMap<>();
        for (Bloque b : this.bloques) {
            this.bloquePorId.put(b.id, b);
        }
        this.cursos = new LinkedHashMap<>();
        for (Curso c : datos.cursos) {
            this.cursos.put(c.id, c);
        }
        this.docentes = new LinkedHashMap<>();
        for (Docente d : datos.docentes) {
            this.docentes.put(d.id, d);
        }
        this.aulas = new LinkedHashMap<>();
        for (Aula a : datos.aulas) {
            this.aulas.put(a.id, a);
        }
        this.aulasLista = List.copyOf(datos.aulas);
        this.topeGrupo = (int) Math.floor(cfg.cupoMaximo * (1.0 + cfg.tolerancia));
    }

    public static List<Bloque> generarBloques(Configuracion cfg) {
        List<Bloque> bloques = new ArrayList<>();
        int orden = 0;
        for (int d = 0; d < 6; d++) {
            Configuracion.RangoHora f = d < 5 ? cfg.franja.semana : cfg.franja.sabado;
            Configuracion.RangoHora a = cfg.almuerzo;
            List<int[]> tramos = new ArrayList<>();
            if (f.inicio < a.inicio) {
                tramos.add(new int[]{f.inicio, Math.min(f.fin, a.inicio)});
            }
            if (f.fin > a.fin) {
                tramos.add(new int[]{Math.max(f.inicio, a.fin), f.fin});
            }
            for (int[] tramo : tramos) {
                int i = tramo[0];
                int j = tramo[1];
                for (int h = i; h + cfg.duracionSesion <= j; h += cfg.duracionSesion) {
                    bloques.add(new Bloque(d + "-" + h, d, NOMBRES_DIA[d], h, h + cfg.duracionSesion, orden++));
                }
            }
        }
        return bloques;
    }

    public int capacidadRequerida(Curso c) {
        return Math.min(c.inscritos, this.topeGrupo);
    }

    public int topeDocente(Docente d) {
        return this.cfg.dedicacion.getOrDefault(d.dedicacion, 0);
    }

    public boolean admiteDocente(Docente d, Bloque b) {
        if (d == null || b == null) return false;
        Docente.Disponibilidad disp = d.disponibilidad;
        if (disp == null) return true;
        if (disp.dias != null && !disp.dias.contains(b.dia)) return false;
        if (disp.desde != null && b.inicio < disp.desde) return false;
        if (disp.hasta != null && b.fin > disp.hasta) return false;
        return true;
    }
}
