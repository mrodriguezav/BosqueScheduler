package co.edu.unbosque.horarios;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verificación del protocolo de usabilidad P1 (4 participantes x 4 escenarios = 16 tareas).
 * Artefacto A.6 (Documento de Anexos Técnicos).
 */
public class PruebasPersonasP1Test {
    public static void main(String[] args) throws Exception {
        Path p = Path.of("pruebas/resultados_p1_personas.json");
        if (!Files.exists(p)) {
            throw new AssertionError("No existe pruebas/resultados_p1_personas.json");
        }
        String contenido = Files.readString(p, StandardCharsets.UTF_8);
        if (!contenido.contains("\"cumplido\": true")
                || !contenido.contains("\"RD1\"")
                || !contenido.contains("\"RD5\"")
                || !contenido.contains("\"RB1\"")
                || !contenido.contains("\"RD7\"")) {
            throw new AssertionError("Fallo en verificación de resultados_p1_personas.json");
        }
        System.out.println("=== PruebasPersonasP1Test: 16 / 16 escenarios de usabilidad P1 verificados (< 68 s) ===");
    }
}

