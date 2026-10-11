package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.service.Auditor;
import co.edu.unbosque.horarios.service.Auditor.ResultadoAuditoria;
import co.edu.unbosque.horarios.service.GeneradorVoraz.CursoSinUbicar;
import co.edu.unbosque.horarios.service.GeneradorVoraz.ResultadoGeneracion;
import co.edu.unbosque.horarios.service.SistemaHorarios;
import java.util.ArrayList;

public class PruebaBasicaTest {
    public static void main(String[] args) {
        SistemaHorarios s = SistemaHorarios.crearSistema();
        Horario h = s.nuevoHorario();
        ResultadoGeneracion r = s.generador.generar(h, new ArrayList<>(s.ctx.cursos.keySet()));
        ResultadoAuditoria aud = Auditor.auditar(h);

        System.out.println("=== Prueba Básica (Java 21 / Spring Boot) ===");
        System.out.println("Bloques por semana: " + s.ctx.bloques.size());
        System.out.println("Sesiones ubicadas: " + r.ubicadas + " de 41");
        for (CursoSinUbicar u : r.sinUbicar) {
            System.out.println("Sin ubicar: " + u.cursoId + " (" + u.nombre + ") -> " + u.causa);
        }
        System.out.println("Auditoría total violaciones: " + aud.total);
        if (s.ctx.bloques.size() != 32 || r.ubicadas != 39 || aud.total != 0) {
            throw new AssertionError("Fallo en PruebaBasicaTest");
        }
    }
}
