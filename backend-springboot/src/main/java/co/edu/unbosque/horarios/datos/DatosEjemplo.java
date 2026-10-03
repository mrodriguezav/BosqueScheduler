package co.edu.unbosque.horarios.datos;

import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Docente;
import co.edu.unbosque.horarios.model.Docente.Disponibilidad;
import java.util.ArrayList;
import java.util.List;

/**
 * Conjunto de ejemplo sintético del equipo: 20 cursos, 41 sesiones, 12 docentes, 12 aulas.
 */
public class DatosEjemplo {

    public static class ConjuntoDatos {
        public final List<Aula> aulas;
        public final List<Docente> docentes;
        public final List<Curso> cursos;

        public ConjuntoDatos(List<Aula> aulas, List<Docente> docentes, List<Curso> cursos) {
            this.aulas = List.copyOf(aulas);
            this.docentes = List.copyOf(docentes);
            this.cursos = List.copyOf(cursos);
        }
    }

    public static ConjuntoDatos base() {
        List<Aula> aulas = List.of(
            new Aula("E1", "estandar", 45), new Aula("E2", "estandar", 45),
            new Aula("E3", "estandar", 45), new Aula("E4", "estandar", 50),
            new Aula("E5", "estandar", 50),
            new Aula("C1", "computo", 34), new Aula("C2", "computo", 34),
            new Aula("C3", "computo", 32), new Aula("C4", "computo", 30),
            new Aula("M1", "movil", 44), new Aula("M2", "movil", 40),
            new Aula("M3", "movil", 40)
        );

        List<Docente> docentes = List.of(
            new Docente("D1", "TC"),
            new Docente("D2", "TC", new Disponibilidad(null, null, 13)),
            new Docente("D3", "TC"),
            new Docente("D4", "TC", new Disponibilidad(null, 16, null)),
            new Docente("D5", "TQ"),
            new Docente("D6", "TQ", new Disponibilidad(List.of(0, 1, 2, 3), null, null)),
            new Docente("D7", "TQ"),
            new Docente("D8", "MT"),
            new Docente("D9", "MT", new Disponibilidad(null, null, 13)),
            new Docente("D10", "MT", new Disponibilidad(null, 13, null)),
            new Docente("D11", "UQ"),
            new Docente("D12", "UQ", new Disponibilidad(List.of(1, 3), null, null))
        );

        List<Curso> cursos = List.of(
            new Curso("K01", "Programación I", "computo", 30, 4, List.of("D1", "D3", "D7")),
            new Curso("K02", "Cálculo Diferencial", "estandar", 42, 3, List.of("D1", "D5", "D6")),
            new Curso("K03", "Física Mecánica", "estandar", 44, 3, List.of("D4")),
            new Curso("K04", "Estructuras de Datos", "computo", 32, 3, List.of("D1", "D3", "D7")),
            new Curso("K05", "Ingeniería de Software I", "movil", 40, 3, List.of("D3", "D5", "D8")),
            new Curso("K06", "Álgebra Lineal", "estandar", 40, 2, List.of("D2", "D6", "D9")),
            new Curso("K07", "Matemáticas Discretas", "estandar", 38, 2, List.of("D2", "D5", "D9")),
            new Curso("K08", "Redes de Datos I", "computo", 34, 2, List.of("D3", "D7", "D10")),
            new Curso("K09", "Bases de Datos", "computo", 44, 2, List.of("D3", "D7", "D8")),
            new Curso("K10", "Sistemas Operativos", "estandar", 40, 2, List.of("D1", "D7", "D10")),
            new Curso("K11", "Probabilidad y Estadística", "estandar", 44, 2, List.of("D2", "D6", "D12")),
            new Curso("K12", "Dinámica de Sistemas", "movil", 38, 2, List.of("D5", "D8", "D11")),
            new Curso("K13", "Modelos y Simulación", "movil", 36, 2, List.of("D5", "D8", "D12")),
            new Curso("K14", "Complejidad Algorítmica", "estandar", 38, 2, List.of("D1", "D3", "D10")),
            new Curso("K15", "Arquitectura de Computadores", "computo", 30, 2, List.of("D7", "D10", "D12")),
            new Curso("K16", "Ética Profesional", "estandar", 44, 1, List.of("D9", "D11", "D12")),
            new Curso("K17", "Inglés Técnico", "estandar", 35, 1, List.of("D9", "D11", "D12")),
            new Curso("K18", "Investigación Tecnológica", "movil", 40, 1, List.of("D5", "D8", "D11")),
            new Curso("K19", "Electiva Humanística", "estandar", 40, 1, List.of("D6", "D11", "D12")),
            new Curso("K20", "Seminario de Grado", "estandar", 25, 1, List.of("D1", "D8", "D10"))
        );

        return new ConjuntoDatos(aulas, docentes, cursos);
    }

    private static String idc(String id, int c) {
        return c == 0 ? id : id + "_" + c;
    }

    public static ConjuntoDatos replicar(ConjuntoDatos base, int k, boolean mezclar) {
        List<Aula> aulas = new ArrayList<>();
        List<Docente> docentes = new ArrayList<>();
        List<Curso> cursos = new ArrayList<>();

        for (int c = 0; c < k; c++) {
            for (Aula a : base.aulas) {
                aulas.add(new Aula(idc(a.id, c), a.tipo, a.capacidad));
            }
            for (Docente d : base.docentes) {
                docentes.add(new Docente(idc(d.id, c), d.dedicacion, d.disponibilidad));
            }
            for (Curso x : base.cursos) {
                List<String> hab = new ArrayList<>();
                for (String h : x.habilitados) {
                    if (mezclar) {
                        for (int j = 0; j < k; j++) {
                            hab.add(idc(h, j));
                        }
                    } else {
                        hab.add(idc(h, c));
                    }
                }
                cursos.add(new Curso(idc(x.id, c), x.nombre, x.tipoAula, x.inscritos, x.frecuencia, hab));
            }
        }
        return new ConjuntoDatos(aulas, docentes, cursos);
    }
}
