package com.alexis;

import com.alexis.config.AppConfig;
import java.util.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppConfigTest {
    private Map<String, String> valores() {
        return new HashMap<>(Map.of("APP_HOST", "127.0.0.1", "PORT", "9001",
            "DB_URL", "jdbc:postgresql://example.invalid:5432/prueba", "DB_USER", "usuario-prueba", "DB_PASSWORD", "secreto-prueba"));
    }
    @Test public void usaHostPuertoYLimitesConfigurados() {
        var valores = valores(); valores.put("DB_POOL_SIZE", "3"); valores.put("HTTP_THREADS", "4");
        var config = AppConfig.desde(valores);
        assertEquals(9001, config.port()); assertEquals("127.0.0.1", config.host());
        assertEquals(3, config.poolSize()); assertEquals(4, config.httpThreads());
        assertFalse(config.toString().contains("secreto-prueba"));
        assertFalse(config.toString().contains("example.invalid"));
    }
    @Test public void rechazaConfiguracionObligatoriaIncompleta() {
        for (String clave : List.of("APP_HOST", "PORT", "DB_URL", "DB_USER", "DB_PASSWORD")) {
            var valores = valores(); valores.remove(clave);
            try { AppConfig.desde(valores); fail("Aceptó falta de " + clave); }
            catch (IllegalArgumentException error) { assertTrue(error.getMessage().contains(clave)); }
        }
    }
    @Test public void rechazaPuertosYLimitesInvalidosSinExponerValores() {
        for (String clave : List.of("PORT", "DB_POOL_SIZE", "HTTP_THREADS", "DB_QUERY_TIMEOUT_SECONDS")) {
            for (String valor : List.of("0", "999999", "secreto-prueba")) {
                var valores = valores(); valores.put(clave, valor);
                try { AppConfig.desde(valores); fail("Aceptó " + clave); }
                catch (IllegalArgumentException error) { assertFalse(error.getMessage().contains("secreto-prueba")); }
            }
        }
    }
    @Test public void rechazaCredencialesEnLaUrl() {
        var valores = valores(); valores.put("DB_URL", "jdbc:postgresql://example.invalid/base?password=secreto-prueba");
        try { AppConfig.desde(valores); fail("Aceptó secreto en URL"); }
        catch (IllegalArgumentException error) { assertFalse(error.getMessage().contains("secreto-prueba")); }
    }
    @Test public void cargaArchivoSeleccionadoSinMezclarOtraBase() throws Exception {
        Path directorio = Files.createTempDirectory("cogas-config-");
        Path local = directorio.resolve(".env");
        Path nube = directorio.resolve(".env.supabase");
        try {
            String comunes = "APP_HOST=127.0.0.1\nPORT=9001\nDB_USER=usuario-prueba\nDB_PASSWORD=secreto-prueba\n";
            Files.writeString(local, comunes + "DB_URL=jdbc:postgresql://local.invalid/local\n");
            Files.writeString(nube, comunes + "DB_URL=jdbc:postgresql://cloud.invalid/postgres?sslmode=require\n");
            assertEquals("jdbc:postgresql://local.invalid/local", AppConfig.cargar(local, Map.of()).dbUrl());
            assertEquals("jdbc:postgresql://cloud.invalid/postgres?sslmode=require", AppConfig.cargar(nube, Map.of()).dbUrl());
            assertEquals("jdbc:postgresql://local.invalid/local", AppConfig.cargar(local, Map.of()).dbUrl());
        } finally {
            Files.deleteIfExists(local); Files.deleteIfExists(nube); Files.deleteIfExists(directorio);
        }
    }
    @Test public void entornoTienePrioridadSobreElArchivoSeleccionado() throws Exception {
        Path archivo = Files.createTempFile("cogas-config-", ".env");
        try {
            Files.writeString(archivo, "APP_HOST=127.0.0.1\nPORT=9001\nDB_URL=jdbc:postgresql://cloud.invalid/postgres\nDB_USER=usuario-prueba\nDB_PASSWORD=secreto-prueba\n");
            assertEquals(9002, AppConfig.cargar(archivo, Map.of("PORT", "9002")).port());
            var entorno = valores(); entorno.put("PORT", "9003");
            assertEquals(9003, AppConfig.cargar(null, entorno).port());
        } finally { Files.deleteIfExists(archivo); }
    }
    @Test public void archivoSeleccionadoInexistenteNoUsaOtraConfiguracion() throws Exception {
        Path directorio = Files.createTempDirectory("cogas-config-");
        try {
            AppConfig.cargar(directorio.resolve(".env.supabase"), valores());
            fail("Aceptó un archivo seleccionado inexistente.");
        } catch (IllegalArgumentException error) {
            assertEquals("No existe el archivo de configuración seleccionado.", error.getMessage());
        } finally { Files.deleteIfExists(directorio); }
    }
}
