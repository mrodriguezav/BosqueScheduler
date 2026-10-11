package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.datos.DatosEjemplo.ConjuntoDatos;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.service.Auditor;
import co.edu.unbosque.horarios.service.Auditor.ResultadoAuditoria;
import co.edu.unbosque.horarios.service.GeneradorVoraz.CursoSinUbicar;
import co.edu.unbosque.horarios.service.GeneradorVoraz.ResultadoGeneracion;
import co.edu.unbosque.horarios.service.SistemaHorarios;
import co.edu.unbosque.horarios.service.ValidadorMonolitico;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

public class EjecutarP2Test {

    private static double mediana(List<Double> vals) {
        List<Double> s = new ArrayList<>(vals);
        Collections.sort(s);
        return s.get(s.size() / 2);
    }

    private static long medianaLong(List<Long> vals) {
        List<Long> s = new ArrayList<>(vals);
        Collections.sort(s);
        return s.get(s.size() / 2);
    }

    private static double pct(List<Double> vals, double p) {
        List<Double> s = new ArrayList<>(vals);
        Collections.sort(s);
        int idx = Math.min(s.size() - 1, (int) Math.ceil(p * s.size()) - 1);
        return s.get(idx);
    }

    private static String huella(Horario h) {
        return h.getSesiones().values().stream()
            .map(x -> x.id + "|" + x.docenteId + "|" + x.aulaId + "|" + x.bloqueId)
            .sorted()
            .collect(Collectors.joining(";"));
    }

    public static void main(String[] args) throws Exception {
        // Calentamiento JIT de la JVM para mediciones estables
        for (int i = 0; i < 30; i++) {
            SistemaHorarios w = SistemaHorarios.crearSistema();
            Horario hw = w.nuevoHorario();
            w.generador.generarConRetroceso(hw, new ArrayList<>(w.ctx.cursos.keySet()));
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // 1. Treinta órdenes aleatorios (v1 y v2)
        for (String[] par : new String[][]{{"v1", "generar"}, {"v2", "generarConRetroceso"}}) {
            String clave = par[0];
            boolean usarRetroceso = "v2".equals(clave);

            int conTodasFactibles = 0;
            int bdReportado = 0;
            int conViolaciones = 0;
            int ubicMin = Integer.MAX_VALUE;
            int ubicMax = 0;
            int retroMax = 0;
            int identicos = 0;
            List<Double> tiemposMs = new ArrayList<>();
            List<Long> evals = new ArrayList<>();
            List<String> fallosJson = new ArrayList<>();

            for (int seed = 1; seed <= 30; seed++) {
                SistemaHorarios s = SistemaHorarios.crearSistema();
                Horario h = s.nuevoHorario();
                List<String> orden = PrngUtil.barajar(new ArrayList<>(s.ctx.cursos.keySet()), PrngUtil.prng(seed));
                long t0 = System.nanoTime();
                ResultadoGeneracion r = usarRetroceso
                    ? s.generador.generarConRetroceso(h, orden)
                    : s.generador.generar(h, orden);
                long t1 = System.nanoTime();
                ResultadoAuditoria aud = Auditor.auditar(h);

                SistemaHorarios s2 = SistemaHorarios.crearSistema(DatosEjemplo.base(), Configuracion.porDefecto(), false);
                Horario h2 = s2.nuevoHorario();
                if (usarRetroceso) s2.generador.generarConRetroceso(h2, orden);
                else s2.generador.generar(h2, orden);

                if (huella(h).equals(huella(h2))) identicos++;

                List<String> extra = r.sinUbicar.stream()
                    .map(x -> x.cursoId)
                    .filter(id -> !"K09".equals(id))
                    .collect(Collectors.toList());
                CursoSinUbicar bd = r.sinUbicar.stream().filter(x -> "K09".equals(x.cursoId)).findFirst().orElse(null);

                if (extra.isEmpty()) conTodasFactibles++;
                else {
                    String exStr = extra.stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(","));
                    fallosJson.add("{\"seed\":" + seed + ",\"extra\":[" + exStr + "]}");
                }
                if (bd != null && bd.causa.startsWith("Sin aula compatible")) bdReportado++;
                if (aud.total > 0) conViolaciones++;
                ubicMin = Math.min(ubicMin, r.ubicadas);
                ubicMax = Math.max(ubicMax, r.ubicadas);
                retroMax = Math.max(retroMax, r.retrocesos);
                tiemposMs.add((t1 - t0) / 1_000_000.0);
                evals.add(r.evaluaciones);
            }

            json.append("  \"ordenes_").append(clave).append("\": {\n")
                .append("    \"corridas\": 30,\n")
                .append("    \"conTodasLasFactibles\": ").append(conTodasFactibles).append(",\n")
                .append("    \"bdReportadoConCausa\": ").append(bdReportado).append(",\n")
                .append("    \"corridasConViolaciones\": ").append(conViolaciones).append(",\n")
                .append("    \"ubicadasMin\": ").append(ubicMin).append(",\n")
                .append("    \"ubicadasMax\": ").append(ubicMax).append(",\n")
                .append("    \"msMediana\": ").append(String.format(Locale.US, "%.4f", mediana(tiemposMs))).append(",\n")
                .append("    \"msMax\": ").append(String.format(Locale.US, "%.4f", Collections.max(tiemposMs))).append(",\n")
                .append("    \"evalMediana\": ").append(medianaLong(evals)).append(",\n")
                .append("    \"evalMax\": ").append(Collections.max(evals)).append(",\n")
                .append("    \"retrocesosMax\": ").append(retroMax).append(",\n")
                .append("    \"identicosConYSinPrefiltro\": ").append(identicos).append(",\n")
                .append("    \"fallos\": [").append(String.join(", ", fallosJson)).append("]\n")
                .append("  },\n");
        }

        // 2. Cien movimientos aleatorios
        {
            SistemaHorarios s = SistemaHorarios.crearSistema();
            Horario h = s.nuevoHorario();
            s.generador.generar(h, new ArrayList<>(s.ctx.cursos.keySet()));
            DoubleSupplier rnd = PrngUtil.prng(2026);
            List<Double> tiempos = new ArrayList<>();
            int aceptados = 0, rechazados = 0, conAviso = 0, cruces = 0;
            Map<String, Integer> motivos = new LinkedHashMap<>();

            for (int i = 0; i < 100; i++) {
                List<String> ids = new ArrayList<>(h.getSesiones().keySet());
                Sesion s0 = h.obtenerSesion(ids.get((int) Math.floor(rnd.getAsDouble() * ids.size())));
                Bloque b = s.ctx.bloques.get((int) Math.floor(rnd.getAsDouble() * s.ctx.bloques.size()));
                boolean cambiaAula = rnd.getAsDouble() < 0.3;
                String aula = cambiaAula
                    ? s.ctx.aulasLista.get((int) Math.floor(rnd.getAsDouble() * s.ctx.aulasLista.size())).id
                    : s0.aulaId;
                Sesion cand = new Sesion(s0.id, s0.cursoId, s0.docenteId, aula, b.id);
                long t0 = System.nanoTime();
                List<Violacion> viol = s.validador.evaluar(cand, h);
                List<Violacion> duras = viol.stream().filter(v -> "dura".equals(v.severidad)).collect(Collectors.toList());
                if (duras.isEmpty()) {
                    h.mover(s0.id, b.id, aula);
                }
                long t1 = System.nanoTime();
                tiempos.add((t1 - t0) / 1_000_000.0);
                if (!duras.isEmpty()) {
                    rechazados++;
                    for (Violacion v : duras) {
                        motivos.put(v.regla, motivos.getOrDefault(v.regla, 0) + 1);
                    }
                } else {
                    aceptados++;
                    if (!viol.isEmpty()) conAviso++;
                }
                ResultadoAuditoria a = Auditor.auditar(h);
                cruces += a.crucesDocente + a.crucesAula;
                if (a.total > 0) throw new AssertionError("Auditoría con violaciones tras el movimiento " + i);
            }

            long menoresA200 = tiempos.stream().filter(t -> t < 200.0).count();
            String motivosStr = motivos.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\": " + e.getValue())
                .collect(Collectors.joining(", "));

            json.append("  \"movimientos\": {\n")
                .append("    \"total\": 100,\n")
                .append("    \"aceptados\": ").append(aceptados).append(",\n")
                .append("    \"rechazados\": ").append(rechazados).append(",\n")
                .append("    \"aceptadosConAviso\": ").append(conAviso).append(",\n")
                .append("    \"crucesTrasAuditar\": ").append(cruces).append(",\n")
                .append("    \"motivosRechazo\": {").append(motivosStr).append("},\n")
                .append("    \"msMediana\": ").append(String.format(Locale.US, "%.6f", mediana(tiempos))).append(",\n")
                .append("    \"msP95\": ").append(String.format(Locale.US, "%.6f", pct(tiempos, 0.95))).append(",\n")
                .append("    \"msMax\": ").append(String.format(Locale.US, "%.6f", Collections.max(tiempos))).append(",\n")
                .append("    \"menoresA200ms\": ").append(menoresA200).append("\n")
                .append("  },\n");
        }

        // 3. Strategy frente a función monolítica (20.000 candidatas)
        {
            SistemaHorarios s = SistemaHorarios.crearSistema();
            Horario h = s.nuevoHorario();
            s.generador.generar(h, new ArrayList<>(s.ctx.cursos.keySet()));
            DoubleSupplier rnd = PrngUtil.prng(7);
            List<String> ids = new ArrayList<>(h.getSesiones().keySet());
            List<Sesion> cands = new ArrayList<>(20000);
            for (int i = 0; i < 20000; i++) {
                Sesion s0 = h.obtenerSesion(ids.get((int) Math.floor(rnd.getAsDouble() * ids.size())));
                Curso curso = s.ctx.cursos.get(s0.cursoId);
                String docId = rnd.getAsDouble() < 0.7
                    ? s0.docenteId
                    : curso.habilitados.get((int) Math.floor(rnd.getAsDouble() * curso.habilitados.size()));
                String bloqId = s.ctx.bloques.get((int) Math.floor(rnd.getAsDouble() * s.ctx.bloques.size())).id;
                String aulaId = s.ctx.aulasLista.get((int) Math.floor(rnd.getAsDouble() * s.ctx.aulasLista.size())).id;
                cands.add(new Sesion(s0.id, s0.cursoId, docId, aulaId, bloqId));
            }
            int discrepancias = 0;
            for (Sesion c : cands) {
                if (s.validador.esValida(c, h) != ValidadorMonolitico.validarMonolitico(c, h)) {
                    discrepancias++;
                }
            }
            int rondas = 100;
            double totalEval = (double) cands.size() * rondas;

            // Warmup
            for (int i = 0; i < 5; i++) {
                for (Sesion c : cands) {
                    s.validador.esValida(c, h);
                    ValidadorMonolitico.validarMonolitico(c, h);
                    s.validador.evaluar(c, h);
                }
            }

            long bestStrat = Long.MAX_VALUE;
            for (int rep = 0; rep < 3; rep++) {
                long t0 = System.nanoTime();
                for (int r = 0; r < rondas; r++) for (Sesion c : cands) s.validador.esValida(c, h);
                bestStrat = Math.min(bestStrat, System.nanoTime() - t0);
            }
            long bestMono = Long.MAX_VALUE;
            for (int rep = 0; rep < 3; rep++) {
                long t0 = System.nanoTime();
                for (int r = 0; r < rondas; r++) for (Sesion c : cands) ValidadorMonolitico.validarMonolitico(c, h);
                bestMono = Math.min(bestMono, System.nanoTime() - t0);
            }
            long bestEval = Long.MAX_VALUE;
            for (int rep = 0; rep < 3; rep++) {
                long t0 = System.nanoTime();
                for (int r = 0; r < rondas; r++) for (Sesion c : cands) s.validador.evaluar(c, h);
                bestEval = Math.min(bestEval, System.nanoTime() - t0);
            }

            double nsStrategy = bestStrat / totalEval;
            double nsMono = bestMono / totalEval;
            double nsEvaluar = bestEval / totalEval;

            json.append("  \"strategy\": {\n")
                .append("    \"candidatas\": ").append(cands.size()).append(",\n")
                .append("    \"discrepancias\": ").append(discrepancias).append(",\n")
                .append("    \"nsStrategyEsValida\": ").append(String.format(Locale.US, "%.2f", nsStrategy)).append(",\n")
                .append("    \"nsMonolitico\": ").append(String.format(Locale.US, "%.2f", nsMono)).append(",\n")
                .append("    \"nsStrategyEvaluarCompleto\": ").append(String.format(Locale.US, "%.2f", nsEvaluar)).append(",\n")
                .append("    \"razon\": ").append(String.format(Locale.US, "%.2f", nsStrategy / Math.max(1.0, nsMono))).append("\n")
                .append("  },\n");
        }

        // 4. Escalabilidad
        {
            json.append("  \"escalabilidad\": [\n");
            List<String> filasJson = new ArrayList<>();
            for (boolean mezclar : new boolean[]{false, true}) {
                for (boolean pre : new boolean[]{false, true}) {
                    for (int k : new int[]{1, 2, 4, 8, 16, 32}) {
                        ConjuntoDatos datos = DatosEjemplo.replicar(DatosEjemplo.base(), k, mezclar);
                        List<Double> tg = new ArrayList<>();
                        List<Long> ev = new ArrayList<>();
                        int ubic = 0;
                        long extra = 0;
                        int viol = 0;
                        double tMov = 0;
                        double tAud = 0;

                        for (int rep = 0; rep < 3; rep++) {
                            SistemaHorarios s = SistemaHorarios.crearSistema(datos, Configuracion.porDefecto(), pre);
                            Horario h = s.nuevoHorario();
                            List<String> orden = PrngUtil.barajar(new ArrayList<>(s.ctx.cursos.keySet()), PrngUtil.prng(11));
                            long t0 = System.nanoTime();
                            ResultadoGeneracion r = s.generador.generarConRetroceso(h, orden);
                            long t1 = System.nanoTime();
                            tg.add((t1 - t0) / 1_000_000.0);
                            ev.add(r.evaluaciones);
                            ubic = r.ubicadas;
                            extra = r.sinUbicar.stream().filter(x -> !x.cursoId.startsWith("K09")).count();
                            if (rep == 0) {
                                DoubleSupplier rnd = PrngUtil.prng(5);
                                List<String> ids = new ArrayList<>(h.getSesiones().keySet());
                                List<Double> tt = new ArrayList<>();
                                for (int i = 0; i < 200; i++) {
                                    Sesion s0 = h.obtenerSesion(ids.get((int) Math.floor(rnd.getAsDouble() * ids.size())));
                                    String bId = s.ctx.bloques.get((int) Math.floor(rnd.getAsDouble() * s.ctx.bloques.size())).id;
                                    Sesion cand = new Sesion(s0.id, s0.cursoId, s0.docenteId, s0.aulaId, bId);
                                    long a = System.nanoTime();
                                    s.validador.evaluar(cand, h);
                                    tt.add((System.nanoTime() - a) / 1_000_000.0);
                                }
                                tMov = mediana(tt);
                                long a2 = System.nanoTime();
                                ResultadoAuditoria au = Auditor.auditar(h);
                                tAud = (System.nanoTime() - a2) / 1_000_000.0;
                                viol = au.total;
                            }
                        }
                        filasJson.add(String.format(Locale.US,
                            "    {\"modo\":\"%s\",\"prefiltro\":%b,\"k\":%d,\"cursos\":%d,\"docentes\":%d,\"aulas\":%d,\"sesiones\":%d,\"ubicadas\":%d,\"sinUbicarFactibles\":%d,\"generarMs\":%.3f,\"evaluaciones\":%d,\"movimientoMs\":%.6f,\"auditoriaMs\":%.3f,\"violaciones\":%d}",
                            mezclar ? "compartidos" : "independientes", pre, k, 20 * k, 12 * k, 12 * k, 41 * k,
                            ubic, extra, mediana(tg), medianaLong(ev), tMov, tAud, viol
                        ));
                    }
                }
            }
            json.append(String.join(",\n", filasJson)).append("\n  ]\n}");
        }

        Path outPath = Path.of("pruebas/resultados_p2.json");
        Files.writeString(outPath, json.toString(), StandardCharsets.UTF_8);
        System.out.println("=== EjecutarP2Test completado: pruebas/resultados_p2.json generado ===");
    }
}
