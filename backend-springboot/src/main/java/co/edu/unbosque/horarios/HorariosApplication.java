package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.controller.ServidorHttpSpringBoot;

/**
 * Punto de entrada de la aplicación Java 21 / Spring Boot.
 * Compatible tanto con Spring Boot (mvn spring-boot:run) como con ejecución directa en JVM (java HorariosApplication).
 */
public class HorariosApplication {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        for (String arg : args) {
            if (arg.startsWith("--server.port=")) {
                port = Integer.parseInt(arg.substring("--server.port=".length()));
            }
        }
        ServidorHttpSpringBoot.iniciar(port);
    }
}
