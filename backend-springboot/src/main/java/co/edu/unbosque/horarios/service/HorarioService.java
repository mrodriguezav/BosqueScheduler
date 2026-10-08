package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.service.Auditor.ResultadoAuditoria;
import co.edu.unbosque.horarios.service.GeneradorVoraz.ResultadoGeneracion;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de aplicación de Spring (@Service) que administra la sesión de planeación actual.
 */
public class HorarioService {
    private SistemaHorarios sistema;
    private Horario horarioActual;
    private ResultadoGeneracion ultimoResultado;

    public HorarioService() {
        reiniciar(Configuracion.porDefecto(), true);
    }

    public synchronized ResultadoGeneracion reiniciar(Configuracion cfg, boolean usarRetroceso) {
        this.sistema = SistemaHorarios.crearSistema(DatosEjemplo.base(), cfg, true);
        this.horarioActual = this.sistema.nuevoHorario();
        List<String> orden = new ArrayList<>(this.sistema.ctx.cursos.keySet());
        this.ultimoResultado = usarRetroceso
            ? this.sistema.generador.generarConRetroceso(this.horarioActual, orden)
            : this.sistema.generador.generar(this.horarioActual, orden);
        return this.ultimoResultado;
    }

    public synchronized SistemaHorarios getSistema() {
        return sistema;
    }

    public synchronized Horario getHorarioActual() {
        return horarioActual;
    }

    public synchronized ResultadoGeneracion getUltimoResultado() {
        return ultimoResultado;
    }

    public static class RespuestaMovimiento {
        public final boolean aceptado;
        public final List<Violacion> violaciones;
        public final ResultadoAuditoria auditoria;
        public final double tiempoMs;

        public RespuestaMovimiento(boolean aceptado, List<Violacion> violaciones, ResultadoAuditoria auditoria, double tiempoMs) {
            this.aceptado = aceptado;
            this.violaciones = violaciones;
            this.auditoria = auditoria;
            this.tiempoMs = tiempoMs;
        }
    }

    public synchronized RespuestaMovimiento intentarMover(String sesionId, String nuevoBloqueId, String nuevaAulaId) {
        long t0 = System.nanoTime();
        Sesion actual = horarioActual.obtenerSesion(sesionId);
        if (actual == null) {
            return new RespuestaMovimiento(false, List.of(new Violacion("SIS", "dura", "Sesión no encontrada: " + sesionId)), Auditor.auditar(horarioActual), 0.0);
        }
        String aulaDestino = (nuevaAulaId != null && !nuevaAulaId.isEmpty()) ? nuevaAulaId : actual.aulaId;
        Sesion candidata = new Sesion(actual.id, actual.cursoId, actual.docenteId, aulaDestino, nuevoBloqueId);
        List<Violacion> violaciones = sistema.validador.evaluar(candidata, horarioActual);
        boolean tieneDuras = violaciones.stream().anyMatch(v -> "dura".equals(v.severidad));
        if (!tieneDuras) {
            horarioActual.mover(sesionId, nuevoBloqueId, aulaDestino);
        }
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        return new RespuestaMovimiento(!tieneDuras, violaciones, Auditor.auditar(horarioActual), ms);
    }
}
