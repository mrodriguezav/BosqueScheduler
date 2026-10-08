package co.edu.unbosque.horarios.controller;

import co.edu.unbosque.horarios.service.HorarioService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador HTTP embebido para el controlador REST (@RestController) y recursos estáticos de Angular.
 */
public class ServidorHttpSpringBoot {

    private static final String JSON = "application/json; charset=utf-8";

    public static HttpServer iniciar(int port) throws IOException {
        HorarioService service = new HorarioService();
        HorarioController controller = new HorarioController(service);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        server.createContext("/api/horario", exchange -> {
            try {
                agregarCors(exchange);
                String metodo = exchange.getRequestMethod();
                if ("OPTIONS".equalsIgnoreCase(metodo)) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                params.putAll(parseJsonSimple(body));

                String respuesta;
                if (path.equals("/api/horario/mover")) {
                    if (!"POST".equalsIgnoreCase(metodo)) {
                        enviarError(exchange, 405, "Use POST para /api/horario/mover");
                        return;
                    }
                    respuesta = controller.moverSesionJson(
                        params.getOrDefault("sesionId", ""),
                        params.getOrDefault("bloqueId", ""),
                        params.getOrDefault("aulaId", "")
                    );
                } else if (path.equals("/api/horario/reiniciar") || path.equals("/api/horario/generar")) {
                    if (!"POST".equalsIgnoreCase(metodo)) {
                        enviarError(exchange, 405, "Use POST para " + path);
                        return;
                    }
                    int cupo = parseEntero(params, "cupoMaximo", 40);
                    String rb1 = params.getOrDefault("severidadRb1", "aviso");
                    int ini = parseEntero(params, "inicioSemana", 7);
                    respuesta = controller.reiniciarJson(cupo, rb1, ini);
                } else if (path.equals("/api/horario")) {
                    if (!"GET".equalsIgnoreCase(metodo)) {
                        enviarError(exchange, 405, "Use GET para /api/horario");
                        return;
                    }
                    respuesta = controller.obtenerEstadoJson();
                } else {
                    enviarError(exchange, 404, "Ruta no encontrada: " + path);
                    return;
                }
                enviarBytes(exchange, 200, JSON, respuesta.getBytes(StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                enviarError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                enviarError(exchange, 500, "Error interno: " + e.getClass().getSimpleName());
            } finally {
                exchange.close();
            }
        });

        server.createContext("/", exchange -> {
            try {
                agregarCors(exchange);
                String reqPath = exchange.getRequestURI().getPath();
                if ("/".equals(reqPath)) reqPath = "/index.html";
                Path base = Path.of(".").toAbsolutePath().normalize();
                Path file = base.resolve(reqPath.substring(1)).normalize();
                if (!file.startsWith(base)) {
                    enviarBytes(exchange, 403, "text/plain; charset=utf-8", "Prohibido".getBytes(StandardCharsets.UTF_8));
                } else if (Files.exists(file) && !Files.isDirectory(file)) {
                    String mime = reqPath.endsWith(".js") ? "application/javascript; charset=utf-8"
                                : reqPath.endsWith(".css") ? "text/css; charset=utf-8"
                                : reqPath.endsWith(".json") ? JSON
                                : "text/html; charset=utf-8";
                    enviarBytes(exchange, 200, mime, Files.readAllBytes(file));
                } else {
                    enviarBytes(exchange, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
                }
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("Servidor Backend Java (Spring Boot REST API) activo en http://127.0.0.1:" + port);
        return server;
    }

    private static void agregarCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void enviarBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void enviarError(HttpExchange exchange, int status, String mensaje) throws IOException {
        String m = mensaje == null ? "" : mensaje.replace("\\", "\\\\").replace("\"", "\\\"");
        enviarBytes(exchange, status, JSON, ("{\"error\":\"" + m + "\"}").getBytes(StandardCharsets.UTF_8));
    }

    private static int parseEntero(Map<String, String> params, String clave, int porDefecto) {
        String v = params.get(clave);
        if (v == null || v.isBlank()) return porDefecto;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El parámetro " + clave + " debe ser un número entero: '" + v + "'");
        }
    }

    private static Map<String, String> parseQuery(String q) {
        Map<String, String> map = new HashMap<>();
        if (q == null || q.isEmpty()) return map;
        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static Map<String, String> parseJsonSimple(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        String clean = json.trim().replace("{", "").replace("}", "").replace("\"", "");
        for (String part : clean.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}
