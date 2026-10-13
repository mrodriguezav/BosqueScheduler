package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.model.Contexto;
import co.edu.unbosque.horarios.model.Horario;
import co.edu.unbosque.horarios.model.Sesion;
import co.edu.unbosque.horarios.model.Violacion;
import co.edu.unbosque.horarios.reglas.Regla;
import co.edu.unbosque.horarios.service.Auditor;
import co.edu.unbosque.horarios.service.GeneradorVoraz;
import co.edu.unbosque.horarios.service.SistemaHorarios;
import co.edu.unbosque.horarios.service.ValidadorRestricciones;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VerificarDiagramasTest {
    private static int ok = 0;
    private static final List<String> fallos = new ArrayList<>();

    private static void chk(String desc, boolean cond) {
        if (cond) ok++;
        else fallos.add(desc);
    }

    private static boolean tieneMetodo(Class<?> cls, String nombre) {
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(nombre)) return true;
        }
        return false;
    }

    private static boolean tieneCampo(Class<?> cls, String nombre) {
        for (Field f : cls.getDeclaredFields()) {
            if (f.getName().equals(nombre)) return true;
        }
        return false;
    }

    private static boolean esCampoPrivado(Class<?> cls, String nombre) {
        for (Field f : cls.getDeclaredFields()) {
            if (f.getName().equals(nombre)) return Modifier.isPrivate(f.getModifiers());
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        SistemaHorarios sis = SistemaHorarios.crearSistema();
        Horario h = sis.nuevoHorario();

        // 1. Métodos de las clases principales (17 checks)
        for (String m : new String[]{"colocar", "quitar", "mover", "docenteEn", "aulaEn", "cargaDe", "sesionesDeCurso"}) {
            chk("Horario." + m + " existe", tieneMetodo(Horario.class, m));
        }
        for (String m : new String[]{"evaluar", "esValida", "esValidaSinAula", "esValidaConAula"}) {
            chk("ValidadorRestricciones." + m + " existe", tieneMetodo(ValidadorRestricciones.class, m));
        }
        for (String m : new String[]{"generar", "generarConRetroceso", "ubicarCurso", "colocarDia"}) {
            chk("GeneradorVoraz." + m + " existe", tieneMetodo(GeneradorVoraz.class, m));
        }
        for (String m : new String[]{"evaluar", "violacion"}) {
            chk("Regla." + m + " existe", tieneMetodo(Regla.class, m));
        }

        // 2. Atributos de Horario (privados en Java 21), Contexto y GeneradorVoraz (19 checks)
        for (String a : new String[]{"ctx", "sesiones", "ocDocente", "ocAula", "carga", "porCurso"}) {
            chk("Horario." + a + " (private final)", tieneCampo(Horario.class, a) && esCampoPrivado(Horario.class, a));
        }
        for (String a : new String[]{"cfg", "bloques", "bloquePorId", "cursos", "docentes", "aulas", "topeGrupo"}) {
            chk("Contexto." + a, tieneCampo(Contexto.class, a));
        }
        for (String m : new String[]{"capacidadRequerida", "topeDocente", "admiteDocente"}) {
            chk("Contexto." + m + "()", tieneMetodo(Contexto.class, m));
        }
        for (String a : new String[]{"validador", "ctx", "prefiltro"}) {
            chk("GeneradorVoraz." + a, tieneCampo(GeneradorVoraz.class, a));
        }

        // 3. Reglas y patrón Strategy (20 checks)
        chk("ValidadorRestricciones.reglas", tieneCampo(ValidadorRestricciones.class, "reglas"));
        chk("Validador agrega 9 reglas (1..*)", sis.validador.reglas.size() == 9);
        chk("todas las reglas extienden Regla", sis.validador.reglas.stream().allMatch(r -> r instanceof Regla));
        Map<String, String> esperadas = Map.of(
            "CruceDocente", "RD1",
            "CruceAula", "RD2",
            "DocenteHabilitado", "RD3",
            "FranjaHoraria", "RD4",
            "AulaCompatible", "RD5",
            "DuracionSesion", "RD6",
            "DisponibilidadDocente", "RD7",
            "TopeDedicacion", "RD8",
            "DiasConsecutivos", "RB1"
        );
        for (Regla r : sis.validador.reglas) {
            String n = r.getClass().getSimpleName();
            chk(n + " es " + esperadas.get(n), r.codigo.equals(esperadas.get(n)));
        }
        chk("Regla es clase abstracta", Modifier.isAbstract(Regla.class.getModifiers()));
        String conAula = sis.validador.reglas.stream().filter(r -> r.usaAula).map(r -> r.codigo).sorted().collect(Collectors.joining(","));
        chk("usaAula solo en RD2 y RD5", "RD2,RD5".equals(conAula));
        chk("RB1 es aviso por configuración", "aviso".equals(sis.validador.reglas.stream().filter(r -> "RB1".equals(r.codigo)).findFirst().get().severidad));

        sis.generador.generarConRetroceso(h, new ArrayList<>(sis.ctx.cursos.keySet()));
        Sesion s0 = h.obtenerSesion("K02#0");
        List<Violacion> viol = sis.validador.evaluar(new Sesion(s0.id, s0.cursoId, "D1", "C1", s0.bloqueId), h);
        chk("Violacion tiene regla, severidad y motivo", !viol.isEmpty() && tieneCampo(Violacion.class, "regla") && tieneCampo(Violacion.class, "severidad") && tieneCampo(Violacion.class, "motivo"));
        chk("Sesion referencia por id (cursoId, docenteId, aulaId, bloqueId)",
            Arrays.asList("id", "cursoId", "docenteId", "aulaId", "bloqueId").stream().allMatch(k -> tieneCampo(Sesion.class, k)));
        chk("Auditor.auditar es función de utilidad", tieneMetodo(Auditor.class, "auditar"));

        String srcGen = Files.readString(Path.of("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/GeneradorVoraz.java"));
        String srcVal = Files.readString(Path.of("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/ValidadorRestricciones.java"));
        String srcAud = Files.readString(Path.of("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/Auditor.java"));
        chk("Generador y Validador no importan reglas concretas",
            !srcGen.contains("CruceDocente") && !srcVal.contains("CruceDocente"));
        chk("Auditor no importa reglas ni usa los índices del Horario",
            !srcAud.contains("reglas") && !srcAud.contains("ocDocente") && !srcAud.contains("ocAula"));

        // 4. Diagrama de secuencia: traza de un movimiento rechazado y uno aceptado (8 checks) -> Total = 17 + 19 + 20 + 8 = 64 checks!
        List<String> traza = new ArrayList<>();
        Horario.trazaActiva = traza::add;

        String huellaAntes = h.getSesiones().values().stream().map(x -> x.id + "@" + x.bloqueId).sorted().collect(Collectors.joining(";"));
        Sesion k03 = h.obtenerSesion("K03#0");
        Sesion rech = new Sesion(k03.id, k03.cursoId, k03.docenteId, k03.aulaId, "0-9"); // D4 solo después de las 16:00 (RD7)
        List<Violacion> vRech = sis.validador.evaluar(rech, h);
        String ordenReglas = traza.stream().filter(t -> t.startsWith("Regla.")).map(t -> t.substring(6)).collect(Collectors.joining(","));
        String esperadoReglas = sis.validador.reglas.stream().map(r -> r.codigo).collect(Collectors.joining(","));
        chk("Secuencia 2: evaluar recorre las 9 reglas en el orden registrado", ordenReglas.equals(esperadoReglas));
        chk("Secuencia 3.1: las reglas consultan el Horario", traza.contains("Horario.consulta"));
        chk("Secuencia 4: el rechazo trae una violación dura", vRech.stream().anyMatch(x -> "dura".equals(x.severidad)));
        String huellaDespuesRech = h.getSesiones().values().stream().map(x -> x.id + "@" + x.bloqueId).sorted().collect(Collectors.joining(";"));
        chk("Secuencia 5a: en rechazo no se llama a mover y el estado no cambia",
            !traza.contains("Horario.mover") && huellaAntes.equals(huellaDespuesRech));

        traza.clear();
        Sesion k05 = h.obtenerSesion("K05#2");
        Sesion acep = new Sesion(k05.id, k05.cursoId, k05.docenteId, k05.aulaId, "1-15");
        List<Violacion> vAcep = sis.validador.evaluar(acep, h);
        boolean sinDuras = vAcep.stream().noneMatch(x -> "dura".equals(x.severidad));
        if (sinDuras) h.mover(k05.id, "1-15", k05.aulaId);
        int idxEval = traza.indexOf("Validador.evaluar");
        int idxMover = traza.indexOf("Horario.mover");
        int idxQuitar = traza.indexOf("Horario.quitar");
        int idxColocar = traza.indexOf("Horario.colocar");
        chk("Secuencia 5b: mover ocurre después de evaluar", sinDuras && idxEval >= 0 && idxMover > idxEval);
        chk("Secuencia 5b.1: mover invoca internamente quitar y luego colocar", idxQuitar > idxMover && idxColocar > idxQuitar);
        chk("Secuencia 5c: la sesión queda en el nuevo bloque", "1-15".equals(h.obtenerSesion(k05.id).bloqueId));
        chk("Auditoría tras movimiento sigue en 0 violaciones", Auditor.auditar(h).total == 0);

        Horario.trazaActiva = null;

        String json = String.format("{\n  \"superadas\": %d,\n  \"total\": %d\n}", ok, ok + fallos.size());
        Files.writeString(Path.of("pruebas/resultados_diagramas.json"), json, StandardCharsets.UTF_8);
        System.out.println("=== VerificarDiagramasTest: " + ok + " / " + (ok + fallos.size()) + " comprobaciones superadas ===");
        if (!fallos.isEmpty() || ok != 64) {
            throw new AssertionError("Fallos o conteo distinto de 64 en VerificarDiagramasTest: " + ok + " fallos=" + fallos);
        }
    }
}
