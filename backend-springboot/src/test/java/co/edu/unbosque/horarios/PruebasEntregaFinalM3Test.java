package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.controller.HorarioController;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.service.Auditor;
import co.edu.unbosque.horarios.service.Auditor.ResultadoAuditoria;
import co.edu.unbosque.horarios.service.GeneradorVoraz.ResultadoGeneracion;
import co.edu.unbosque.horarios.service.HorarioService;
import co.edu.unbosque.horarios.service.HorarioService.RespuestaMovimiento;
import co.edu.unbosque.horarios.service.SistemaHorarios;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Suite Integral de Aseguramiento de Calidad (QA) para la Entrega Final del Módulo 3.
 * Verifica las 15 aserciones deterministas (RF1, RD1..RD8, RB1-A, RB1-B, RF3, M3-CARGA, M3-EXP)
 * y genera el registro auditable en pruebas/qa_m3_execution.log.
 */
public class PruebasEntregaFinalM3Test {

    private static int superadas = 0;
    private static final List<String> lineasLog = new ArrayList<>();

    private static void registrar(String linea) {
        System.out.println(linea);
        lineasLog.add(linea);
    }

    private static void verificar(boolean condicion, String id, String desc, String detalle) {
        if (!condicion) {
            throw new AssertionError("Fallo en prueba QA M3 [" + id + "]: " + desc + " -> " + detalle);
        }
        superadas++;
        registrar(String.format("[PASS] %-8s | %-51s | %s", id, desc, detalle));
    }

    private static Violacion buscarRegla(List<Violacion> lista, String codigo) {
        for (Violacion v : lista) {
            if (codigo.equals(v.regla)) return v;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        registrar("=== Ejecutando Suite Integral QA Módulo 3 (PruebasEntregaFinalM3Test) ===");

        SistemaHorarios sis = SistemaHorarios.crearSistema();
        Horario h = sis.nuevoHorario();
        ResultadoGeneracion gen = sis.generador.generarConRetroceso(h, new ArrayList<>(sis.ctx.cursos.keySet()));
        ResultadoAuditoria aud = Auditor.auditar(h);

        // 1. RF1: Generación inicial
        verificar(gen.ubicadas == 39 && gen.sinUbicar.size() == 1 && "K09".equals(gen.sinUbicar.get(0).cursoId) && aud.total == 0,
            "RF1", "Generación inicial con 20 asignaturas",
            "Ubicadas: " + gen.ubicadas + "/41, Sin ubicar: K09 (RD5), Violaciones Auditor: " + aud.total);

        Sesion k01_0 = h.obtenerSesion("K01#0");

        // 2. RD1: Choque de docente mismo bloque
        List<Violacion> vRd1 = sis.validador.evaluar(new Sesion("K02#0", "K02", k01_0.docenteId, "E1", k01_0.bloqueId), h);
        Violacion rd1 = buscarRegla(vRd1, "RD1");
        verificar(rd1 != null && "dura".equals(rd1.severidad),
            "RD1", "Bloqueo de choque de docente mismo bloque",
            rd1 != null ? rd1.motivo : "sin violacion");

        // 3. RD2: Choque de aula mismo bloque
        List<Violacion> vRd2 = sis.validador.evaluar(new Sesion("K04#0", "K04", "D3", k01_0.aulaId, k01_0.bloqueId), h);
        Violacion rd2 = buscarRegla(vRd2, "RD2");
        verificar(rd2 != null && "dura".equals(rd2.severidad),
            "RD2", "Bloqueo de choque de aula mismo bloque",
            rd2 != null ? rd2.motivo : "sin violacion");

        // 4. RD3: Docente no habilitado
        List<Violacion> vRd3 = sis.validador.evaluar(new Sesion("K01#0", "K01", "D12", "C1", "1-9"), h);
        Violacion rd3 = buscarRegla(vRd3, "RD3");
        verificar(rd3 != null && "dura".equals(rd3.severidad),
            "RD3", "Bloqueo de docente no habilitado en curso",
            rd3 != null ? rd3.motivo : "sin violacion");

        // 5. RD4: Franja de almuerzo / inválida
        List<Violacion> vRd4 = sis.validador.evaluar(new Sesion("K01#0", "K01", "D1", "C1", "0-12"), h);
        Violacion rd4 = buscarRegla(vRd4, "RD4");
        verificar(rd4 != null && "dura".equals(rd4.severidad),
            "RD4", "Bloqueo de sesión en franja de almuerzo/inválida",
            rd4 != null ? rd4.motivo : "sin violacion");

        // 6. RD5-A: Capacidad insuficiente (K09 en C1)
        List<Violacion> vRd5a = sis.validador.evaluar(new Sesion("K09#0", "K09", "D3", "C1", "0-13"), h);
        Violacion rd5a = buscarRegla(vRd5a, "RD5");
        verificar(rd5a != null && "dura".equals(rd5a.severidad),
            "RD5-A", "Bloqueo por capacidad insuficiente (K09 en C1)",
            rd5a != null ? rd5a.motivo : "sin violacion");

        // 7. RD5-B: Tipo de aula incompatible
        List<Violacion> vRd5b = sis.validador.evaluar(new Sesion("K01#0", "K01", "D1", "E1", "0-7"), h);
        Violacion rd5b = buscarRegla(vRd5b, "RD5");
        verificar(rd5b != null && "dura".equals(rd5b.severidad),
            "RD5-B", "Bloqueo por tipo de aula incompatible",
            rd5b != null ? rd5b.motivo : "sin violacion");

        // 8. RD6: Duración distinta de 2h
        List<Violacion> vRd6 = sis.validador.evaluar(new Sesion("K01#0", "K01", "D1", "C1", "0-12"), h);
        Violacion rd6 = buscarRegla(vRd6, "RD6");
        verificar(rd6 != null && "dura".equals(rd6.severidad),
            "RD6", "Bloqueo de bloque con duración distinta de 2h",
            rd6 != null ? rd6.motivo : "sin violacion");

        // 9. RD7: Fuera de disponibilidad docente (D4 a las 9:00)
        Sesion k03_0 = h.obtenerSesion("K03#0");
        List<Violacion> vRd7 = sis.validador.evaluar(new Sesion(k03_0.id, k03_0.cursoId, "D4", k03_0.aulaId, "0-9"), h);
        Violacion rd7 = buscarRegla(vRd7, "RD7");
        verificar(rd7 != null && "dura".equals(rd7.severidad),
            "RD7", "Bloqueo fuera de disponibilidad docente (D4 a las 9:00)",
            rd7 != null ? rd7.motivo : "sin violacion");

        // 10. RD8: Superar tope semanal de dedicación (UQ=2 para D12)
        Horario hD12 = sis.nuevoHorario();
        hD12.colocar(new Sesion("K11#0", "K11", "D12", "E1", "1-7"));
        hD12.colocar(new Sesion("K11#1", "K11", "D12", "E1", "3-7"));
        List<Violacion> vRd8 = sis.validador.evaluar(new Sesion("K16#0", "K16", "D12", "E1", "1-9"), hD12);
        Violacion rd8 = buscarRegla(vRd8, "RD8");
        verificar(rd8 != null && "dura".equals(rd8.severidad),
            "RD8", "Bloqueo por superar tope semanal de dedicación (UQ=2)",
            rd8 != null ? rd8.motivo : "sin violacion");

        // 11. RB1-A: Advertencia blanda en días consecutivos (severidad=aviso)
        Sesion k05_2 = h.obtenerSesion("K05#2");
        List<Violacion> vRb1a = sis.validador.evaluar(new Sesion(k05_2.id, "K05", k05_2.docenteId, k05_2.aulaId, "1-15"), h);
        Violacion rb1a = buscarRegla(vRb1a, "RB1");
        verificar(rb1a != null && "aviso".equals(rb1a.severidad),
            "RB1-A", "Advertencia blanda en días consecutivos (severidad=aviso)",
            rb1a != null ? rb1a.motivo : "sin violacion");

        // 12. RB1-B: Bloqueo duro en días consecutivos (severidad=dura)
        Configuracion cfgDura = Configuracion.porDefecto();
        cfgDura.severidades.put("RB1", "dura");
        SistemaHorarios sisDura = SistemaHorarios.crearSistema(DatosEjemplo.base(), cfgDura, true);
        List<Violacion> vRb1b = sisDura.validador.evaluar(new Sesion(k05_2.id, "K05", k05_2.docenteId, k05_2.aulaId, "1-15"), h);
        Violacion rb1b = buscarRegla(vRb1b, "RB1");
        verificar(rb1b != null && "dura".equals(rb1b.severidad),
            "RB1-B", "Bloqueo duro en días consecutivos (severidad=dura)",
            rb1b != null ? rb1b.motivo : "sin violacion");

        // 13. RF3: Reasignación de aula compatible (E4 -> E5)
        HorarioService service = new HorarioService();
        Sesion sK03 = service.getHorarioActual().obtenerSesion("K03#0");
        String aulaDestino = "E5".equals(sK03.aulaId) ? "E4" : "E5";
        RespuestaMovimiento movRf3 = service.intentarMover("K03#0", sK03.bloqueId, aulaDestino);
        verificar(movRf3.aceptado && aulaDestino.equals(service.getHorarioActual().obtenerSesion("K03#0").aulaId),
            "RF3", "Reasignación de aula compatible (E4 -> E5)",
            String.format(Locale.US, "Aula actualizada a %s en %.4f ms", aulaDestino, movRf3.tiempoMs));

        // 14. M3-CARGA: Carga de datos JSON y regeneración sin violaciones
        HorarioController controller = new HorarioController(service);
        String payload = Files.exists(Path.of("datos/datos_prueba_20cursos.json"))
            ? Files.readString(Path.of("datos/datos_prueba_20cursos.json"), StandardCharsets.UTF_8)
            : "{}";
        String jsonCarga = controller.cargarDatosJson(payload);
        verificar(jsonCarga.contains("\"ubicadas\":39") && jsonCarga.contains("\"total\":0"),
            "M3-CARGA", "Carga de datos JSON y regeneración sin violaciones",
            "Endpoint /api/horario/cargar -> ubicadas=39, auditoria.total=0");

        // 15. M3-EXP: Exportación de horario a CSV (encabezado + 39 sesiones = 40 líneas)
        String csv = controller.exportarCsv();
        String[] lineasCsv = csv.trim().split("\r?\n");
        verificar(lineasCsv.length == 40 && lineasCsv[0].startsWith("SesionID,CursoID,Asignatura"),
            "M3-EXP", "Exportación de horario a CSV (encabezado + 39 sesiones)",
            "Líneas CSV generadas: " + lineasCsv.length + " (1 encabezado + 39 sesiones)");

        registrar("=== PruebasEntregaFinalM3Test: " + superadas + " / 15 pruebas superadas ===");
        Files.writeString(Path.of("pruebas/qa_m3_execution.log"), String.join("\n", lineasLog) + "\n", StandardCharsets.UTF_8);
    }
}
