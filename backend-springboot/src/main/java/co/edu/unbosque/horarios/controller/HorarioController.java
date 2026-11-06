package co.edu.unbosque.horarios.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Bloque;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Docente;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.service.Auditor;
import co.edu.unbosque.horarios.service.Auditor.ResultadoAuditoria;
import co.edu.unbosque.horarios.service.GeneradorVoraz.CursoSinUbicar;
import co.edu.unbosque.horarios.service.GeneradorVoraz.ResultadoGeneracion;
import co.edu.unbosque.horarios.service.HorarioService;
import co.edu.unbosque.horarios.service.HorarioService.RespuestaMovimiento;
import co.edu.unbosque.horarios.service.SistemaHorarios;

/**
 * Controlador REST (@RestController) que expone el motor Java 21 / Spring Boot al cliente Angular y consola web.
 * Rutas activas en el Módulo 3:
 *  - GET  /api/horario          -> Estado completo del horario, contexto, sesiones, sinUbicar y auditoría
 *  - POST /api/horario/mover    -> Evalúa y aplica (si no hay violación dura) un traslado de bloque (RF2) o cambio de aula (RF3)
 *  - POST /api/horario/reiniciar-> Regenera el horario con configuración opcional (RF6)
 *  - POST /api/horario/cargar   -> Carga conjunto de datos JSON (20 asignaturas, 12 docentes, 12 aulas) y ejecuta generarConRetroceso (RF1)
 *  - GET  /api/horario/exportar -> Exporta horario validado a CSV (40 líneas) o JSON auditado (RNF4)
 */
public class HorarioController {
    private final HorarioService service;

    public HorarioController(HorarioService service) {
        this.service = service;
    }

    public String obtenerEstadoJson() {
        SistemaHorarios sis = service.getSistema();
        Horario h = service.getHorarioActual();
        ResultadoGeneracion gen = service.getUltimoResultado();
        ResultadoAuditoria aud = Auditor.auditar(h);

        int totalSesiones = 0;
        for (Curso c : sis.ctx.cursos.values()) totalSesiones += c.frecuencia;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"archivo\":\"datos_prueba_20cursos.json\",");
        sb.append("\"tiempoMs\":").append(String.format(Locale.US, "%.4f", service.getUltimoTiempoCargaMs())).append(",");
        sb.append("\"ubicadas\":").append(h.totalSesiones()).append(",");
        sb.append("\"totalSesiones\":").append(totalSesiones).append(",");
        sb.append("\"evaluaciones\":").append(gen.evaluaciones).append(",");
        sb.append("\"retrocesos\":").append(gen.retrocesos).append(",");

        // bloques
        sb.append("\"bloques\":[");
        boolean first = true;
        for (Bloque b : sis.ctx.bloques) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(b.id)
              .append("\",\"dia\":").append(b.dia)
              .append(",\"nombreDia\":\"").append(b.nombreDia)
              .append("\",\"inicio\":").append(b.inicio)
              .append(",\"fin\":").append(b.fin)
              .append(",\"orden\":").append(b.orden).append("}");
        }
        sb.append("],");

        // cursos
        sb.append("\"cursos\":[");
        first = true;
        for (Curso c : sis.ctx.cursos.values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(c.id)
              .append("\",\"nombre\":\"").append(esc(c.nombre))
              .append("\",\"tipoAula\":\"").append(c.tipoAula)
              .append("\",\"inscritos\":").append(c.inscritos)
              .append(",\"frecuencia\":").append(c.frecuencia).append("}");
        }
        sb.append("],");

        // docentes
        sb.append("\"docentes\":[");
        first = true;
        for (Docente d : sis.ctx.docentes.values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(d.id)
              .append("\",\"dedicacion\":\"").append(d.dedicacion)
              .append("\",\"carga\":").append(h.cargaDe(d.id))
              .append(",\"tope\":").append(sis.ctx.topeDocente(d));
            if (d.disponibilidad != null) {
                sb.append(",\"disponibilidad\":{");
                sb.append("\"desde\":").append(d.disponibilidad.desde != null ? d.disponibilidad.desde : "null").append(",");
                sb.append("\"hasta\":").append(d.disponibilidad.hasta != null ? d.disponibilidad.hasta : "null").append(",");
                if (d.disponibilidad.dias != null) {
                    sb.append("\"dias\":").append(d.disponibilidad.dias.toString());
                } else {
                    sb.append("\"dias\":null");
                }
                sb.append("}");
            } else {
                sb.append(",\"disponibilidad\":null");
            }
            sb.append("}");
        }
        sb.append("],");

        // aulas
        sb.append("\"aulas\":[");
        first = true;
        for (Aula a : sis.ctx.aulasLista) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(a.id)
              .append("\",\"tipo\":\"").append(a.tipo)
              .append("\",\"capacidad\":").append(a.capacidad).append("}");
        }
        sb.append("],");

        // sesiones
        sb.append("\"sesiones\":[");
        first = true;
        for (Sesion s : h.getSesiones().values()) {
            if (!first) sb.append(",");
            first = false;
            Curso c = sis.ctx.cursos.get(s.cursoId);
            sb.append("{\"id\":\"").append(s.id)
              .append("\",\"cursoId\":\"").append(s.cursoId)
              .append("\",\"cursoNombre\":\"").append(esc(c != null ? c.nombre : s.cursoId))
              .append("\",\"docenteId\":\"").append(s.docenteId)
              .append("\",\"aulaId\":\"").append(s.aulaId)
              .append("\",\"bloqueId\":\"").append(s.bloqueId).append("\"}");
        }
        sb.append("],");

        // sinUbicar
        sb.append("\"sinUbicar\":[");
        first = true;
        for (CursoSinUbicar u : gen.sinUbicar) {
            if (!first) sb.append(",");
            first = false;
            Curso c = sis.ctx.cursos.get(u.cursoId);
            sb.append("{\"cursoId\":\"").append(u.cursoId)
              .append("\",\"nombre\":\"").append(esc(u.nombre))
              .append("\",\"inscritos\":").append(c != null ? c.inscritos : 0)
              .append(",\"causa\":\"").append(esc(u.causa)).append("\"}");
        }
        sb.append("],");

        // auditoria
        sb.append("\"auditoria\":{\"crucesDocente\":").append(aud.crucesDocente)
          .append(",\"crucesAula\":").append(aud.crucesAula)
          .append(",\"total\":").append(aud.total).append("}");

        sb.append("}");
        return sb.toString();
    }

    public String moverSesionJson(String sesionId, String bloqueId, String aulaId) {
        RespuestaMovimiento r = service.intentarMover(sesionId, bloqueId, aulaId);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"aceptado\":").append(r.aceptado)
          .append(",\"tiempoMs\":").append(String.format(Locale.US, "%.4f", r.tiempoMs))
          .append(",\"violaciones\":[");
        boolean first = true;
        for (Violacion v : r.violaciones) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"regla\":\"").append(v.regla)
              .append("\",\"severidad\":\"").append(v.severidad)
              .append("\",\"motivo\":\"").append(esc(v.motivo)).append("\"}");
        }
        sb.append("],\"auditoria\":{\"crucesDocente\":").append(r.auditoria.crucesDocente)
          .append(",\"crucesAula\":").append(r.auditoria.crucesAula)
          .append(",\"total\":").append(r.auditoria.total).append("},")
          .append("\"estado\":").append(obtenerEstadoJson())
          .append("}");
        return sb.toString();
    }

    public String reiniciarJson(int cupoMaximo, String severidadRb1, int inicioSemana) {
        Configuracion cfg = Configuracion.porDefecto();
        if (cupoMaximo <= 0) {
            throw new IllegalArgumentException("cupoMaximo debe ser un entero positivo");
        }
        if (severidadRb1 != null && !severidadRb1.isEmpty()
                && !"aviso".equals(severidadRb1) && !"dura".equals(severidadRb1)) {
            throw new IllegalArgumentException("severidadRb1 debe ser 'aviso' o 'dura'");
        }
        if (inicioSemana < 0 || inicioSemana + cfg.duracionSesion > cfg.franja.semana.fin) {
            throw new IllegalArgumentException("inicioSemana debe estar entre 0 y " + (cfg.franja.semana.fin - cfg.duracionSesion));
        }
        cfg.cupoMaximo = cupoMaximo;
        if (severidadRb1 != null && !severidadRb1.isEmpty()) cfg.severidades.put("RB1", severidadRb1);
        cfg.franja.semana.inicio = inicioSemana;
        service.reiniciar(cfg, true);
        return obtenerEstadoJson();
    }

    public String cargarDatosJson(String payload) {
        service.cargarDatos(payload);
        return obtenerEstadoJson();
    }

    public String exportarCsv() {
        SistemaHorarios sis = service.getSistema();
        Horario h = service.getHorarioActual();

        StringBuilder sb = new StringBuilder();
        sb.append("SesionID,CursoID,Asignatura,DocenteID,Dedicacion,AulaID,TipoAula,CapacidadAula,BloqueID,Dia,HoraInicio,HoraFin\n");

        List<Sesion> lista = new ArrayList<>(h.getSesiones().values());
        lista.sort(Comparator.comparing(s -> s.id));

        for (Sesion s : lista) {
            Curso c = sis.ctx.cursos.get(s.cursoId);
            Docente d = sis.ctx.docentes.get(s.docenteId);
            Aula a = sis.ctx.aulas.get(s.aulaId);
            Bloque b = sis.ctx.bloquePorId.get(s.bloqueId);

            String nombreCurso = c != null ? c.nombre.replace(",", " ") : s.cursoId;
            String dedicacion = d != null ? d.dedicacion : "";
            String tipoAula = a != null ? a.tipo : "";
            int capAula = a != null ? a.capacidad : 0;
            String nombreDia = b != null ? b.nombreDia : "";
            String horaIni = b != null ? (b.inicio + ":00") : "";
            String horaFin = b != null ? (b.fin + ":00") : "";

            sb.append(s.id).append(",")
              .append(s.cursoId).append(",")
              .append(nombreCurso).append(",")
              .append(s.docenteId).append(",")
              .append(dedicacion).append(",")
              .append(s.aulaId).append(",")
              .append(tipoAula).append(",")
              .append(capAula).append(",")
              .append(s.bloqueId).append(",")
              .append(nombreDia).append(",")
              .append(horaIni).append(",")
              .append(horaFin).append("\n");
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
