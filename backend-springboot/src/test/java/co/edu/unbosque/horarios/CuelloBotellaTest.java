package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.reglas.CatalogoReglas;
import co.edu.unbosque.horarios.reglas.Regla;
import co.edu.unbosque.horarios.service.GeneradorVoraz;
import co.edu.unbosque.horarios.service.ValidadorRestricciones;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CuelloBotellaTest {
    public static void main(String[] args) throws Exception {
        List<String> items = new ArrayList<>();
        for (int k : new int[]{1, 8, 32}) {
            Contexto ctx = new Contexto(DatosEjemplo.replicar(DatosEjemplo.base(), k, true), Configuracion.porDefecto());
            Map<String, Long> cont = new LinkedHashMap<>();
            long[] totales = new long[]{0, 0}; // [total, aceptadas]

            ValidadorRestricciones val = new ValidadorRestricciones(CatalogoReglas.crearReglas(ctx.cfg)) {
                @Override
                public boolean esValida(Sesion c, Horario hh) {
                    totales[0]++;
                    for (Regla r : this.reglas) {
                        if (!"dura".equals(r.severidad)) continue;
                        if (!r.evaluar(c, hh).isEmpty()) {
                            cont.put(r.codigo, cont.getOrDefault(r.codigo, 0L) + 1L);
                            return false;
                        }
                    }
                    totales[1]++;
                    return true;
                }
            };

            GeneradorVoraz gen = new GeneradorVoraz(val, ctx, false);
            Horario h = new Horario(ctx);
            long t0 = System.nanoTime();
            gen.generarConRetroceso(h, PrngUtil.barajar(new ArrayList<>(ctx.cursos.keySet()), PrngUtil.prng(11)));
            double ms = (System.nanoTime() - t0) / 1_000_000.0;

            long total = totales[0];
            long aceptadas = totales[1];
            double porCurso = (double) total / (20 * k);
            String rechStr = cont.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(","));

            items.add(String.format(Locale.US,
                "  {\"k\":%d,\"total\":%d,\"aceptadas\":%d,\"porCurso\":%.1f,\"ms\":%.1f,\"rechazosPorRegla\":{%s},\"pctRD7\":%.1f,\"pctRD1\":%.1f,\"pctRD2\":%.1f,\"pctRD8\":%.1f}",
                k, total, aceptadas, porCurso, ms, rechStr,
                100.0 * cont.getOrDefault("RD7", 0L) / total,
                100.0 * cont.getOrDefault("RD1", 0L) / total,
                100.0 * cont.getOrDefault("RD2", 0L) / total,
                100.0 * cont.getOrDefault("RD8", 0L) / total
            ));
        }
        String json = "[\n" + String.join(",\n", items) + "\n]";
        Files.writeString(Path.of("pruebas/resultados_cuello.json"), json, StandardCharsets.UTF_8);
        System.out.println("=== CuelloBotellaTest completado: pruebas/resultados_cuello.json generado ===");
    }
}
