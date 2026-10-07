package co.edu.unbosque.horarios.reglas;

import co.edu.unbosque.horarios.model.Aula;
import co.edu.unbosque.horarios.model.Curso;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import java.util.List;

public class AulaCompatible extends Regla {
    public AulaCompatible() {
        super("RD5", "dura", true);
    }

    @Override
    public List<Violacion> evaluar(Sesion c, Horario h) {
        if (Horario.trazaActiva != null) Horario.trazaActiva.registrar("Regla." + this.codigo);
        Curso curso = h.getCtx().cursos.get(c.cursoId);
        Aula aula = h.getCtx().aulas.get(c.aulaId);
        if (aula == null) {
            return violacion("El aula " + c.aulaId + " no existe en el catálogo de aulas");
        }
        if (!aula.tipo.equals(curso.tipoAula)) {
            return violacion("El aula " + aula.id + " es de tipo " + aula.tipo + " y el curso requiere " + curso.tipoAula);
        }
        int req = h.getCtx().capacidadRequerida(curso);
        return aula.capacidad >= req
            ? List.of()
            : violacion("El aula " + aula.id + " tiene " + aula.capacidad + " puestos y el curso requiere " + req);
    }
}
