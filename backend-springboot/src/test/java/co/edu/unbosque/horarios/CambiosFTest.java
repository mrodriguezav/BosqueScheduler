package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.reglas.CatalogoReglas;
import co.edu.unbosque.horarios.reglas.Regla;
import co.edu.unbosque.horarios.service.Auditor;
import co.edu.unbosque.horarios.service.GeneradorVoraz;
import co.edu.unbosque.horarios.service.GeneradorVoraz.ResultadoGeneracion;
import co.edu.unbosque.horarios.service.SistemaHorarios;
import co.edu.unbosque.horarios.service.ValidadorRestricciones;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CambiosFTest {

    public static class MaxSesionesPorDia extends Regla {
        public final int max;

        public MaxSesionesPorDia(int max) {
            super("RD9", "dura", false);
            this.max = max;
        }

        @Override
        public List<Violacion> evaluar(Sesion c, Horario h) {
            Bloque b = h.getCtx().bloquePorId.get(c.bloqueId);
            if (b == null) return List.of();
            int n = 0;
            for (Sesion s : h.getSesiones().values()) {
                if (s.id.equals(c.id) || !s.docenteId.equals(c.docenteId)) continue;
                Bloque bs = h.getCtx().bloquePorId.get(s.bloqueId);
                if (bs != null && bs.dia == b.dia) n++;
            }
            return (n + 1 <= this.max)
                ? List.of()
                : violacion("El docente " + c.docenteId + " superaría " + this.max + " sesiones en el mismo día");
        }
    }

    private static String resumen(Configuracion cfg) {
        int ubicMin = Integer.MAX_VALUE;
        int ubicMax = 0;
        int viol = 0;
        int bloques = 0;
        Set<String> sinUbicar = new LinkedHashSet<>();

        for (int seed = 1; seed <= 30; seed++) {
            SistemaHorarios s = SistemaHorarios.crearSistema(DatosEjemplo.base(), cfg, true);
            Horario h = s.nuevoHorario();
            bloques = s.ctx.bloques.size();
            ResultadoGeneracion r = s.generador.generarConRetroceso(h, PrngUtil.barajar(new ArrayList<>(s.ctx.cursos.keySet()), PrngUtil.prng(seed)));
            ubicMin = Math.min(ubicMin, r.ubicadas);
            ubicMax = Math.max(ubicMax, r.ubicadas);
            viol += Auditor.auditar(h).total;
            r.sinUbicar.forEach(x -> sinUbicar.add(x.cursoId));
        }
        String sinStr = sinUbicar.stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(", "));
        return String.format("{\"bloques\":%d,\"ubicadasMin\":%d,\"ubicadasMax\":%d,\"violaciones\":%d,\"sinUbicar\":[%s]}",
            bloques, ubicMin, ubicMax, viol, sinStr);
    }

    /** Copia recursiva portable (reemplaza a `cp -r`, que no existe en Windows). */
    private static void copiarDirectorio(Path origen, Path destino) throws IOException {
        try (Stream<Path> paths = Files.walk(origen)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                Path objetivo = destino.resolve(origen.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(objetivo);
                } else {
                    Files.copy(p, objetivo);
                }
            }
        }
    }

    private static Set<String> rutasRelativas(Path raiz) throws IOException {
        try (Stream<Path> paths = Files.walk(raiz)) {
            return paths.filter(Files::isRegularFile)
                .map(p -> raiz.relativize(p).toString().replace('\\', '/'))
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    /** Comparación portable con la misma salida que `diff -rq` (líneas "Only in" y "Files ... differ"). */
    private static List<String> diferenciasRecursivas(Path a, Path b) throws IOException {
        Set<String> enA = rutasRelativas(a);
        Set<String> enB = rutasRelativas(b);
        List<String> out = new ArrayList<>();
        for (String r : enA) {
            if (!enB.contains(r)) {
                out.add("Only in " + a + ": " + r);
            } else if (!Arrays.equals(Files.readAllBytes(a.resolve(r)), Files.readAllBytes(b.resolve(r)))) {
                out.add("Files " + a.resolve(r) + " and " + b.resolve(r) + " differ");
            }
        }
        for (String r : enB) {
            if (!enA.contains(r)) out.add("Only in " + b + ": " + r);
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        String baseRes = resumen(Configuracion.porDefecto());

        Configuracion cfgA = Configuracion.porDefecto();
        cfgA.cupoMaximo = 30;
        String resA = resumen(cfgA).replace("}", ",\"topeGrupo\":" + (int) Math.floor(30 * 1.1) + "}");

        Configuracion cfgC = Configuracion.porDefecto();
        cfgC.severidades.put("RB1", "dura");
        String resC = resumen(cfgC);

        Configuracion cfgD = Configuracion.porDefecto();
        cfgD.franja.semana.inicio = 6;
        String resD = resumen(cfgD);

        // Cambio B: medir diff real sobre una copia temporal del código Java de reglas/
        Path srcOrig = Path.of("backend-springboot/src/main/java/co/edu/unbosque/horarios");
        Path tmpDir = Files.createTempDirectory("cambioB-java-");
        copiarDirectorio(srcOrig, tmpDir.resolve("horarios"));

        Path rd9File = tmpDir.resolve("horarios/reglas/MaxSesionesPorDia.java");
        Files.writeString(rd9File,
            "package co.edu.unbosque.horarios.reglas;\n" +
            "import co.edu.unbosque.horarios.model.*;\n" +
            "import java.util.List;\n" +
            "public class MaxSesionesPorDia extends Regla {\n" +
            "    public final int max;\n" +
            "    public MaxSesionesPorDia(int max) { super(\"RD9\", \"dura\", false); this.max = max; }\n" +
            "    @Override public List<Violacion> evaluar(Sesion c, Horario h) {\n" +
            "        Bloque b = h.getCtx().bloquePorId.get(c.bloqueId);\n" +
            "        int n = 0;\n" +
            "        for (Sesion s : h.getSesiones().values()) {\n" +
            "            if (s.id.equals(c.id) || !s.docenteId.equals(c.docenteId)) continue;\n" +
            "            if (h.getCtx().bloquePorId.get(s.bloqueId).dia == b.dia) n++;\n" +
            "        }\n" +
            "        return n + 1 <= max ? List.of() : violacion(\"El docente \" + c.docenteId + \" superaría \" + max + \" sesiones en el mismo día\");\n" +
            "    }\n" +
            "}\n", StandardCharsets.UTF_8);

        Path catFile = tmpDir.resolve("horarios/reglas/CatalogoReglas.java");
        String catTxt = Files.readString(catFile);
        catTxt = catTxt.replace(
            "new DiasConsecutivos(cfg.severidades.getOrDefault(\"RB1\", \"aviso\"))",
            "new DiasConsecutivos(cfg.severidades.getOrDefault(\"RB1\", \"aviso\")), new MaxSesionesPorDia(2)"
        );
        Files.writeString(catFile, catTxt, StandardCharsets.UTF_8);

        List<String> diffLines = diferenciasRecursivas(srcOrig, tmpDir.resolve("horarios"));

        long archivosNuevos = diffLines.stream().filter(l -> l.startsWith("Only in")).count();
        long archivosModificados = diffLines.stream().filter(l -> l.startsWith("Files ")).count();

        int maxPorDia = 0;
        int ubicMin = Integer.MAX_VALUE;
        int ubicMax = 0;
        int viol = 0;
        for (int seed = 1; seed <= 30; seed++) {
            Contexto ctx = new Contexto(DatosEjemplo.base(), Configuracion.porDefecto());
            List<Regla> reglasConRd9 = new ArrayList<>(CatalogoReglas.crearReglas(ctx.cfg));
            reglasConRd9.add(new MaxSesionesPorDia(2));
            ValidadorRestricciones val = new ValidadorRestricciones(reglasConRd9);
            GeneradorVoraz gen = new GeneradorVoraz(val, ctx, true);
            Horario h = new Horario(ctx);
            ResultadoGeneracion r = gen.generarConRetroceso(h, PrngUtil.barajar(new ArrayList<>(ctx.cursos.keySet()), PrngUtil.prng(seed)));
            ubicMin = Math.min(ubicMin, r.ubicadas);
            ubicMax = Math.max(ubicMax, r.ubicadas);
            viol += Auditor.auditar(h).total;
            for (String dId : ctx.docentes.keySet()) {
                for (int dia = 0; dia < 6; dia++) {
                    int cDia = 0;
                    for (Sesion s : h.getSesiones().values()) {
                        if (s.docenteId.equals(dId) && ctx.bloquePorId.get(s.bloqueId).dia == dia) cDia++;
                    }
                    maxPorDia = Math.max(maxPorDia, cDia);
                    if (cDia > 2) viol++;
                }
            }
        }
        String diffJson = diffLines.stream().map(l -> "\"" + l.replace("\"", "'") + "\"").collect(Collectors.joining(", "));
        String resB = String.format(
            "{\"archivosDiferentes\":[%s],\"archivosNuevos\":%d,\"archivosModificados\":%d,\"lineasNuevasEnRegistro\":1,\"ubicadasMin\":%d,\"ubicadasMax\":%d,\"violaciones\":%d,\"maxSesionesDocenteDia\":%d}",
            diffJson, archivosNuevos, archivosModificados, ubicMin, ubicMax, viol, maxPorDia
        );

        String json = "{\n"
            + "  \"base\": " + baseRes + ",\n"
            + "  \"A_cupo30\": " + resA + ",\n"
            + "  \"C_rb1_dura\": " + resC + ",\n"
            + "  \"D_franja6\": " + resD + ",\n"
            + "  \"B_rd9\": " + resB + "\n"
            + "}";
        Files.writeString(Path.of("pruebas/resultados_cambios.json"), json, StandardCharsets.UTF_8);
        System.out.println("=== CambiosFTest completado: pruebas/resultados_cambios.json generado ===");
    }
}
