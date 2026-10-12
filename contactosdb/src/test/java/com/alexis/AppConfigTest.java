package com.alexis;

import com.alexis.config.AppConfig;
import java.util.*;
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
}
