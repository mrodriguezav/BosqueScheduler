package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.config.Configuracion;
import co.edu.unbosque.horarios.datos.DatosEjemplo;
import co.edu.unbosque.horarios.datos.DatosEjemplo.ConjuntoDatos;
import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Curso;
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
    private double ultimoTiempoCargaMs = 0.52;

    public HorarioService() {
        reiniciar(Configuracion.porDefecto(), true);
    }

    public synchronized ResultadoGeneracion reiniciar(Configuracion cfg, boolean usarRetroceso) {
        return cargarConjunto(DatosEjemplo.base(), cfg, usarRetroceso);
    }

    public synchronized ResultadoGeneracion cargarConjunto(ConjuntoDatos datos, Configuracion cfg, boolean usarRetroceso) {
        long t0 = System.nanoTime();
        this.sistema = SistemaHorarios.crearSistema(datos, cfg, true);
        this.horarioActual = this.sistema.nuevoHorario();
        List<String> orden = new ArrayList<>(this.sistema.ctx.cursos.keySet());
        this.ultimoResultado = usarRetroceso
            ? this.sistema.generador.generarConRetroceso(this.horarioActual, orden)
            : this.sistema.generador.generar(this.horarioActual, orden);
        this.ultimoTiempoCargaMs = (System.nanoTime() - t0) / 1_000_000.0;
        return this.ultimoResultado;
    }

    public synchronized ResultadoGeneracion cargarDatos(String jsonPayload) {
        Configuracion cfg = Configuracion.porDefecto();
        return cargarConjunto(DatosEjemplo.base(), cfg, true);
    }

    public synchronized double getUltimoTiempoCargaMs() {
        return ultimoTiempoCargaMs;
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
            if (sesionId != null && sesionId.contains("#")) {
                String cursoId = sesionId.substring(0, sesionId.indexOf('#'));
                Curso c = sistema.ctx.cursos.get(cursoId);
                if (c != null) {
                    String docId = c.habilitados.isEmpty() ? "D1" : c.habilitados.get(0);
                    String aulaId = (nuevaAulaId != null && !nuevaAulaId.isEmpty()) ? nuevaAulaId : aulaPorDefectoParaTipo(c.tipoAula);
                    Sesion candNueva = new Sesion(sesionId, cursoId, docId, aulaId, nuevoBloqueId);
                    List<Violacion> viols = sistema.validador.evaluar(candNueva, horarioActual);
                    boolean duras = viols.stream().anyMatch(v -> "dura".equals(v.severidad));
                    if (!duras) {
                        horarioActual.colocar(candNueva);
                    }
                    double ms = (System.nanoTime() - t0) / 1_000_000.0;
                    return new RespuestaMovimiento(!duras, viols, Auditor.auditar(horarioActual), ms);
                }
            }
            return new RespuestaMovimiento(false, List.of(new Violacion("SIS", "dura", "Sesión no encontrada: " + sesionId)), Auditor.auditar(horarioActual), 0.0);
        }
        String bloqueDestino = (nuevoBloqueId != null && !nuevoBloqueId.isEmpty()) ? nuevoBloqueId : actual.bloqueId;
        String aulaDestino = (nuevaAulaId != null && !nuevaAulaId.isEmpty()) ? nuevaAulaId : actual.aulaId;
        Sesion candidata = new Sesion(actual.id, actual.cursoId, actual.docenteId, aulaDestino, bloqueDestino);
        List<Violacion> violaciones = sistema.validador.evaluar(candidata, horarioActual);
        boolean tieneDuras = violaciones.stream().anyMatch(v -> "dura".equals(v.severidad));
        if (!tieneDuras) {
            horarioActual.mover(sesionId, bloqueDestino, aulaDestino);
        }
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        return new RespuestaMovimiento(!tieneDuras, violaciones, Auditor.auditar(horarioActual), ms);
    }

    private String aulaPorDefectoParaTipo(String tipoAula) {
        Aula mejor = null;
        for (Aula a : sistema.ctx.aulasLista) {
            if (a.tipo.equals(tipoAula)) {
                if (mejor == null || a.capacidad > mejor.capacidad) {
                    mejor = a;
                }
            }
        }
        return mejor != null ? mejor.id : (sistema.ctx.aulasLista.isEmpty() ? "C1" : sistema.ctx.aulasLista.get(0).id);
    }
}
