package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Heurística voraz con reintento por patrones de días y retroceso de un nivel (decisión U1).
 */
public class GeneradorVoraz {

    public static class CursoSinUbicar {
        public final String cursoId;
        public final String nombre;
        public final String causa;

        public CursoSinUbicar(String cursoId, String nombre, String causa) {
            this.cursoId = cursoId;
            this.nombre = nombre;
            this.causa = causa;
        }
    }

    public static class ResultadoGeneracion {
        public final List<CursoSinUbicar> sinUbicar;
        public final long evaluaciones;
        public final int ubicadas;
        public final int retrocesos;

        public ResultadoGeneracion(List<CursoSinUbicar> sinUbicar, long evaluaciones, int ubicadas, int retrocesos) {
            this.sinUbicar = sinUbicar;
            this.evaluaciones = evaluaciones;
            this.ubicadas = ubicadas;
            this.retrocesos = retrocesos;
        }
    }

    public static class Estadisticas {
        public long evaluaciones = 0;
    }

    public final ValidadorRestricciones validador;
    public final Contexto ctx;
    public final boolean prefiltro;
    private final Map<String, List<int[]>> cachePatrones = new HashMap<>();

    public GeneradorVoraz(ValidadorRestricciones validador, Contexto ctx, boolean prefiltro) {
        this.validador = validador;
        this.ctx = ctx;
        this.prefiltro = prefiltro;
    }

    public GeneradorVoraz(ValidadorRestricciones validador, Contexto ctx) {
        this(validador, ctx, true);
    }

    private static List<int[]> combinaciones(int n, int k) {
        List<int[]> out = new ArrayList<>();
        combinacionesRec(0, n, k, new ArrayList<>(), out);
        return out;
    }

    private static void combinacionesRec(int ini, int n, int k, List<Integer> acc, List<int[]> out) {
        if (acc.size() == k) {
            int[] arr = new int[k];
            for (int idx = 0; idx < k; idx++) arr[idx] = acc.get(idx);
            out.add(arr);
            return;
        }
        for (int i = ini; i < n; i++) {
            acc.add(i);
            combinacionesRec(i + 1, n, k, acc, out);
            acc.remove(acc.size() - 1);
        }
    }

    private static boolean tieneConsecutivos(int[] p) {
        for (int i = 1; i < p.length; i++) {
            if (p[i] - p[i - 1] == 1) return true;
        }
        return false;
    }

    public List<int[]> patrones(int f, int rotacion) {
        String clave = f + ":" + rotacion;
        if (cachePatrones.containsKey(clave)) return cachePatrones.get(clave);
        List<int[]> todos = combinaciones(6, f);
        List<int[]> ordenados = new ArrayList<>();
        if (f == 2 || f == 3) {
            for (int[] p : todos) if (!tieneConsecutivos(p)) ordenados.add(p);
            for (int[] p : todos) if (tieneConsecutivos(p)) ordenados.add(p);
        } else {
            ordenados.addAll(todos);
        }
        if (ordenados.isEmpty()) {
            cachePatrones.put(clave, ordenados);
            return ordenados;
        }
        int r = rotacion % ordenados.size();
        List<int[]> res;
        if (f == 2 || f == 3) {
            res = ordenados;
        } else {
            res = new ArrayList<>(ordenados.size());
            res.addAll(ordenados.subList(r, ordenados.size()));
            res.addAll(ordenados.subList(0, r));
        }
        cachePatrones.put(clave, res);
        return res;
    }

    public List<Aula> aulasCompatibles(Curso curso) {
        int req = ctx.capacidadRequerida(curso);
        List<Aula> res = new ArrayList<>();
        for (Aula a : ctx.aulasLista) {
            if (a.tipo.equals(curso.tipoAula) && a.capacidad >= req) {
                res.add(a);
            }
        }
        res.sort(Comparator.comparingInt(a -> a.capacidad));
        return res;
    }

    public Sesion colocarDia(Horario h, Curso curso, String docenteId, int dia, int idx, List<Aula> aulas, Estadisticas stats) {
        String id = curso.id + "#" + idx;
        for (Bloque b : ctx.bloques) {
            if (b.dia != dia) continue;
            if (this.prefiltro) {
                stats.evaluaciones++;
                Sesion candSinAula = new Sesion(id, curso.id, docenteId, null, b.id);
                if (!this.validador.esValidaSinAula(candSinAula, h)) continue;
            }
            for (Aula a : aulas) {
                Sesion cand = new Sesion(id, curso.id, docenteId, a.id, b.id);
                stats.evaluaciones++;
                boolean ok = this.prefiltro ? this.validador.esValidaConAula(cand, h) : this.validador.esValida(cand, h);
                if (ok) {
                    h.colocar(cand);
                    return cand;
                }
            }
        }
        return null;
    }

    public boolean ubicarCurso(Horario h, Curso curso, int pos, Estadisticas stats) {
        List<Aula> aulas = aulasCompatibles(curso);
        if (aulas.isEmpty()) return false;
        List<String> docentes = new ArrayList<>(curso.habilitados);
        docentes.sort(Comparator.comparingInt(h::cargaDe));
        for (int[] p : patrones(curso.frecuencia, pos)) {
            for (String d : docentes) {
                List<String> colocadas = new ArrayList<>();
                boolean ok = true;
                for (int i = 0; i < p.length; i++) {
                    Sesion s = colocarDia(h, curso, d, p[i], i, aulas, stats);
                    if (s == null) {
                        ok = false;
                        break;
                    }
                    colocadas.add(s.id);
                }
                if (ok) return true;
                for (String id : colocadas) h.quitar(id);
            }
        }
        return false;
    }

    public String causa(Horario h, Curso curso) {
        List<Aula> aulas = aulasCompatibles(curso);
        if (aulas.isEmpty()) {
            int max = 0;
            for (Aula a : ctx.aulasLista) {
                if (a.tipo.equals(curso.tipoAula)) {
                    max = Math.max(max, a.capacidad);
                }
            }
            return "Sin aula compatible: requiere tipo " + curso.tipoAula + " con " + ctx.capacidadRequerida(curso) + " puestos y el máximo disponible es " + max;
        }
        boolean haySinTope = false;
        for (String d : curso.habilitados) {
            if (h.cargaDe(d) + curso.frecuencia <= ctx.topeDocente(ctx.docentes.get(d))) {
                haySinTope = true;
                break;
            }
        }
        return haySinTope
            ? "Sin bloques disponibles según la disponibilidad de los docentes habilitados y las aulas compatibles"
            : "Los docentes habilitados no tienen tope de sesiones semanales disponible";
    }

    // Versión 1: voraz con reintento por patrones de días.
    public ResultadoGeneracion generar(Horario h, List<String> orden) {
        Estadisticas stats = new Estadisticas();
        List<CursoSinUbicar> sinUbicar = new ArrayList<>();
        for (int pos = 0; pos < orden.size(); pos++) {
            String cursoId = orden.get(pos);
            Curso curso = ctx.cursos.get(cursoId);
            if (!ubicarCurso(h, curso, pos, stats)) {
                sinUbicar.add(new CursoSinUbicar(cursoId, curso.nombre, causa(h, curso)));
            }
        }
        return new ResultadoGeneracion(sinUbicar, stats.evaluaciones, h.totalSesiones(), 0);
    }

    // Versión 2: igual que la 1, y para cada curso sin ubicar intenta un retroceso de un nivel.
    public ResultadoGeneracion generarConRetroceso(Horario h, List<String> orden) {
        ResultadoGeneracion r = generar(h, orden);
        Estadisticas stats = new Estadisticas();
        stats.evaluaciones = r.evaluaciones;
        List<CursoSinUbicar> pendientes = new ArrayList<>();
        int retrocesos = 0;

        for (CursoSinUbicar p : r.sinUbicar) {
            Curso curso = ctx.cursos.get(p.cursoId);
            if (aulasCompatibles(curso).isEmpty()) {
                pendientes.add(p);
                continue;
            }
            boolean reparado = false;
            List<String> ubicados = new ArrayList<>();
            for (String id : orden) {
                if (!id.equals(p.cursoId) && !h.sesionesDeCurso(id).isEmpty()) {
                    ubicados.add(id);
                }
            }
            ubicados.sort((a, b) -> {
                long ca = ctx.cursos.get(a).habilitados.stream().filter(curso.habilitados::contains).count();
                long cb = ctx.cursos.get(b).habilitados.stream().filter(curso.habilitados::contains).count();
                return Long.compare(cb, ca);
            });
            for (String otroId : ubicados) {
                Curso otro = ctx.cursos.get(otroId);
                List<Sesion> guardadas = new ArrayList<>(h.sesionesDeCurso(otroId));
                for (Sesion s : guardadas) h.quitar(s.id);
                retrocesos++;
                if (ubicarCurso(h, curso, 0, stats)) {
                    if (ubicarCurso(h, otro, 0, stats)) {
                        reparado = true;
                        break;
                    }
                    for (Sesion s : new ArrayList<>(h.sesionesDeCurso(p.cursoId))) {
                        h.quitar(s.id);
                    }
                }
                for (Sesion s : guardadas) h.colocar(s);
            }
            if (!reparado) {
                pendientes.add(new CursoSinUbicar(p.cursoId, p.nombre, causa(h, curso)));
            }
        }
        return new ResultadoGeneracion(pendientes, stats.evaluaciones, h.totalSesiones(), retrocesos);
    }
}
