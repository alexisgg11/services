package com.alexis.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.*;
import java.util.*;

public record AppConfig(String host, int port, String dbUrl, String dbUser, String dbPassword,
                        int poolSize, int httpThreads, int queryTimeoutSeconds, int connectionTimeoutSeconds) {
    public static AppConfig cargar() {
        String seleccionado = System.getProperty("app.env.file");
        if (seleccionado != null) return cargar(Path.of(seleccionado), System.getenv());
        Path archivo = Files.exists(Path.of(".env")) ? Path.of(".env") : Path.of("abastecimiento", ".env");
        return cargar(Files.exists(archivo) ? archivo : null, System.getenv());
    }

    public static AppConfig cargar(Path archivo, Map<String, String> entorno) {
        Map<String, String> variables = new HashMap<>();
        if (archivo != null) {
            if (!Files.isRegularFile(archivo))
                throw new IllegalArgumentException("No existe el archivo de configuración seleccionado.");
            Path ruta = archivo.toAbsolutePath().normalize();
            try {
                Dotenv dotenv = Dotenv.configure().directory(ruta.getParent().toString())
                    .filename(ruta.getFileName().toString()).load();
                dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
                    .forEach(e -> variables.put(e.getKey(), e.getValue()));
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("El archivo de configuración tiene un formato inválido.");
            }
        }
        variables.putAll(entorno);
        return desde(variables);
    }

    public static AppConfig desde(Map<String, String> valores) {
        String host = requerida(valores, "APP_HOST");
        if (!host.matches("[a-zA-Z0-9.:-]+")) throw new IllegalArgumentException("APP_HOST no es válido.");
        String url = requerida(valores, "DB_URL");
        if (!url.startsWith("jdbc:postgresql://") || url.matches(".*[?&](user|password)=.*"))
            throw new IllegalArgumentException("DB_URL debe ser JDBC PostgreSQL, sin usuario ni contraseña en la URL.");
        return new AppConfig(host, entero(valores, "PORT", null, 1, 65535), url,
            requerida(valores, "DB_USER"), requerida(valores, "DB_PASSWORD"),
            entero(valores, "DB_POOL_SIZE", "5", 1, 20),
            entero(valores, "HTTP_THREADS", "8", 1, 32),
            entero(valores, "DB_QUERY_TIMEOUT_SECONDS", "10", 1, 60),
            entero(valores, "DB_CONNECTION_TIMEOUT_SECONDS", "20", 3, 60));
    }

    private static String requerida(Map<String, String> valores, String clave) {
        String valor = valores.get(clave);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta configurar " + clave + ".");
        return valor;
    }

    private static int entero(Map<String, String> valores, String clave, String defecto, int min, int max) {
        String valor = valores.getOrDefault(clave, defecto);
        try {
            int numero = Integer.parseInt(valor);
            if (numero >= min && numero <= max) return numero;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException(clave + " debe ser un entero entre " + min + " y " + max + ".");
    }

    @Override public String toString() { return "AppConfig[configuración privada]"; }
}
