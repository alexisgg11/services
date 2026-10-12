package com.alexis.controller;

import com.alexis.service.ProductoService;
import com.alexis.util.Json;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

public class ProductoController implements HttpHandler {
    private final ProductoService servicio;
    public ProductoController(ProductoService servicio) { this.servicio = servicio; }
    @Override public void handle(HttpExchange x) throws IOException {
        try {
            String ruta = x.getRequestURI().getPath();
            String metodo = x.getRequestMethod();
            if (!ruta.startsWith("/api/")) { servirVista(x, ruta, metodo); return; }
            if (!Set.of("GET", "POST", "PUT", "DELETE").contains(metodo)) {
                responder(x, 405, Map.of("error", "Método no permitido.")); return;
            }
            var query = parametros(x.getRequestURI().getRawQuery());
            if (metodo.equals("GET")) {
                Object resultado = switch (ruta) {
                    case "/api/health" -> Map.of("estado", servicio.saludable() ? "ok" : "error", "baseDatos", "PostgreSQL");
                    case "/api/catalogos" -> servicio.catalogos();
                    case "/api/productos" -> servicio.listar(query.getOrDefault("buscar", ""), opcional(query.get("proveedor")), opcional(query.get("tipo")), pagina(query), tamano(query));
                    case "/api/resumen" -> servicio.resumen();
                    case "/api/servicios/producto-mas-caro" -> servicio.masCaro();
                    case "/api/servicios/por-proveedor" -> servicio.porProveedor(ProductoService.idPositivo(query.get("id")), pagina(query), tamano(query));
                    case "/api/servicios/por-tipo" -> servicio.porTipo(ProductoService.idPositivo(query.get("id")), pagina(query), tamano(query));
                    default -> {
                        if (ruta.matches("/api/productos/[0-9]+")) yield servicio.obtener(idRuta(ruta));
                        throw new NoSuchElementException("Ruta no encontrada.");
                    }
                };
                responder(x, 200, resultado); return;
            }
            boolean creacion = ruta.equals("/api/productos") && metodo.equals("POST");
            boolean item = ruta.matches("/api/productos/[0-9]+");
            if (creacion || (item && metodo.equals("PUT"))) {
                String contenido = x.getRequestHeaders().getFirst("Content-Type");
                if (contenido == null || !contenido.startsWith("application/x-www-form-urlencoded")) {
                    responder(x, 415, Map.of("error", "Envía un formulario application/x-www-form-urlencoded.")); return;
                }
                byte[] body = x.getRequestBody().readNBytes(16385);
                if (body.length > 16384) { responder(x, 413, Map.of("error", "Formulario demasiado grande.")); return; }
                int id = servicio.guardar(creacion ? null : idRuta(ruta), parametros(new String(body, StandardCharsets.UTF_8)));
                if (creacion) x.getResponseHeaders().set("Location", "/api/productos/" + id);
                responder(x, creacion ? 201 : 200, Map.of("idProducto", id)); return;
            }
            if (item && metodo.equals("DELETE")) {
                servicio.eliminar(idRuta(ruta)); responder(x, 200, Map.of("mensaje", "Producto eliminado.")); return;
            }
            responder(x, 405, Map.of("error", "Ruta o método no permitido."));
        } catch (IllegalArgumentException error) {
            responder(x, 400, Map.of("error", error.getMessage()));
        } catch (NoSuchElementException error) {
            responder(x, 404, Map.of("error", error.getMessage()));
        } catch (SQLException error) {
            String estado = error.getSQLState();
            boolean conflicto = estado != null && estado.startsWith("23");
            System.err.println("Error PostgreSQL (SQLState=" + estado + ")");
            responder(x, conflicto ? 409 : 503, Map.of("error", conflicto
                ? "Datos relacionados inválidos o restricción de base de datos."
                : "No se pudo consultar PostgreSQL. Revisa la conexión y la inicialización del esquema."));
        } catch (Exception error) {
            System.err.println("Error interno: " + error.getClass().getSimpleName());
            responder(x, 500, Map.of("error", "Error interno del servicio."));
        } finally { x.close(); }
    }
    private static int idRuta(String ruta) { return ProductoService.idPositivo(ruta.substring(ruta.lastIndexOf('/') + 1)); }
    private static Integer opcional(String valor) { return valor == null || valor.isBlank() ? null : ProductoService.idPositivo(valor); }
    private static int pagina(Map<String, String> query) { return ProductoService.idPositivo(query.getOrDefault("pagina", "1")); }
    private static int tamano(Map<String, String> query) { return ProductoService.idPositivo(query.getOrDefault("tamano", "10")); }
    private static Map<String, String> parametros(String texto) {
        var mapa = new HashMap<String, String>();
        if (texto == null || texto.isEmpty()) return mapa;
        if (texto.length() > 16384) throw new IllegalArgumentException("Parámetros demasiado grandes.");
        for (String parte : texto.split("&")) {
            String[] par = parte.split("=", 2);
            String clave = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par.length > 1 ? par[1] : "", StandardCharsets.UTF_8);
            if (mapa.putIfAbsent(clave, valor) != null) throw new IllegalArgumentException("Parámetro repetido: " + clave);
        }
        return mapa;
    }
    private static void responder(HttpExchange x, int estado, Object dato) throws IOException {
        enviar(x, estado, "application/json; charset=utf-8", Json.escribir(dato).getBytes(StandardCharsets.UTF_8));
    }
    private static void enviar(HttpExchange x, int estado, String tipo, byte[] bytes) throws IOException {
        x.getResponseHeaders().set("Content-Type", tipo);
        x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(estado, bytes.length);
        x.getResponseBody().write(bytes);
    }
    private static void servirVista(HttpExchange x, String ruta, String metodo) throws IOException {
        if (!metodo.equals("GET")) { responder(x, 405, Map.of("error", "Método no permitido.")); return; }
        String archivo = switch (ruta) { case "/", "/index.html" -> "index.html"; case "/styles.css" -> "styles.css"; case "/app.js" -> "app.js"; default -> null; };
        if (archivo == null) { responder(x, 404, Map.of("error", "Ruta no encontrada.")); return; }
        String tipo = archivo.endsWith("css") ? "text/css" : archivo.endsWith("js") ? "text/javascript" : "text/html";
        try (var entrada = ProductoController.class.getResourceAsStream("/view/" + archivo)) {
            if (entrada == null) { responder(x, 404, Map.of("error", "Vista no encontrada.")); return; }
            x.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
            enviar(x, 200, tipo + "; charset=utf-8", entrada.readAllBytes());
        }
    }
}
